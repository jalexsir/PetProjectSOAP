#!/usr/bin/env bash
#
# Генерує повний ланцюг довіри для демонстрації TLS / mTLS у rest-service:
#   1) кореневий CA (самопідписаний) — "видавець", якому довіряють і сервер, і клієнт;
#   2) серверний сертифікат (CN=localhost), підписаний CA -> server-keystore.p12
#      (це "особистість" TLS-сервера: пред'являється клієнту під час handshake);
#   3) клієнтський сертифікат (CN=integration-client), підписаний CA -> client-keystore.p12
#      (це "особистість" клієнта для mTLS — сервер вимагає й перевіряє цей сертифікат);
#   4) truststore для сервера й для клієнта — обидва містять лише сертифікат CA,
#      тобто довіряють будь-кому, кого підписав саме цей CA (а не конкретному сертифікату).
#
# Використання:
#   ./certs/generate-certs.sh
#   MTLS_ENABLED=true HUB_... rest-service запускається так:
#   MTLS_ENABLED=true mvn -pl rest-service spring-boot:run
#
# Усі приватні ключі й паролі — ЛИШЕ для локальної навчальної демонстрації.
# У проді: HSM/KMS, короткоживучі сертифікати, окремий CA-процес, паролі в секрет-сховищі.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="${SCRIPT_DIR}/generated"
PASSWORD="${CERTS_PASSWORD:-changeit}"
DAYS_CA=3650
DAYS_LEAF=825   # публічні CA/браузери відмовляють у довших термінах для leaf-сертифікатів; для внутрішнього demo це орієнтир з best practice

rm -rf "${OUT_DIR}"
mkdir -p "${OUT_DIR}"
cd "${OUT_DIR}"

echo "==> 1) Кореневий CA (rootCA.key / rootCA.crt)"
openssl genrsa -out rootCA.key 4096
openssl req -x509 -new -nodes -key rootCA.key -sha256 -days ${DAYS_CA} \
    -subj "/O=PetProject Integration Lab/CN=PetProject Integration Lab Root CA" \
    -out rootCA.crt

echo "==> 2) Серверний сертифікат (CN=localhost), підписаний CA"
openssl genrsa -out server.key 2048
openssl req -new -key server.key \
    -subj "/O=PetProject Integration Lab/CN=localhost" \
    -out server.csr

cat > server-ext.cnf <<EOF
subjectAltName = DNS:localhost, IP:127.0.0.1
extendedKeyUsage = serverAuth
EOF

openssl x509 -req -in server.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial \
    -days ${DAYS_LEAF} -sha256 -extfile server-ext.cnf \
    -out server.crt

echo "==> 3) Клієнтський сертифікат (CN=integration-client), підписаний CA"
openssl genrsa -out client.key 2048
openssl req -new -key client.key \
    -subj "/O=PetProject Integration Lab/CN=integration-client" \
    -out client.csr

cat > client-ext.cnf <<EOF
extendedKeyUsage = clientAuth
EOF

openssl x509 -req -in client.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial \
    -days ${DAYS_LEAF} -sha256 -extfile client-ext.cnf \
    -out client.crt

echo "==> 4) PKCS12 keystore/truststore для сервера (rest-service)"
openssl pkcs12 -export \
    -in server.crt -inkey server.key -certfile rootCA.crt \
    -name server -out server-keystore.p12 -password "pass:${PASSWORD}"

keytool -importcert -noprompt \
    -alias root-ca -file rootCA.crt \
    -keystore server-truststore.p12 -storetype PKCS12 -storepass "${PASSWORD}"

echo "==> 5) PKCS12 keystore/truststore для клієнта (тестовий Java-клієнт mTLS)"
openssl pkcs12 -export \
    -in client.crt -inkey client.key -certfile rootCA.crt \
    -name client -out client-keystore.p12 -password "pass:${PASSWORD}"

keytool -importcert -noprompt \
    -alias root-ca -file rootCA.crt \
    -keystore client-truststore.p12 -storetype PKCS12 -storepass "${PASSWORD}"

rm -f server-ext.cnf client-ext.cnf server.csr client.csr rootCA.srl

echo
echo "Готово. Файли у ${OUT_DIR}:"
ls -1 "${OUT_DIR}"
echo
echo "Пароль keystore/truststore: ${PASSWORD} (змінюється змінною середовища CERTS_PASSWORD)"
echo
echo "Запуск rest-service з увімкненим mTLS-конектором:"
echo "  MTLS_ENABLED=true mvn -pl rest-service spring-boot:run"
echo
echo "Тест mTLS через curl (клієнтський сертифікат + довіра до CA сервера):"
echo "  curl -v https://localhost:8444/api/mtls/orders/ORD-2001 \\"
echo "    --cert-type P12 --cert ${OUT_DIR}/client-keystore.p12:${PASSWORD} \\"
echo "    --cacert ${OUT_DIR}/rootCA.crt"
