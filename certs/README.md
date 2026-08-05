# certs/ — TLS / mTLS для rest-service

`generate-certs.sh` створює локальний ланцюг довіри для демонстрації
адміністрування TLS-сертифікатів та mTLS:

```
rootCA (self-signed)
 ├── server.crt  (CN=localhost)        -> server-keystore.p12 / server-truststore.p12
 └── client.crt  (CN=integration-client) -> client-keystore.p12 / client-truststore.p12
```

## Запуск

```bash
./certs/generate-certs.sh
MTLS_ENABLED=true mvn -pl rest-service spring-boot:run
```

rest-service підніме ДРУГИЙ конектор на `https://localhost:8444` з `client-auth=want`
(окремо від основного HTTP-порту 8082, де далі працюють Basic/OAuth2-демо).

## Перевірка

```bash
# без клієнтського сертифіката -> 401/403 (Spring Security x509 chain)
curl -i --cacert certs/generated/rootCA.crt \
  https://localhost:8444/api/mtls/orders/ORD-2001

# з клієнтським сертифікатом -> 200 + JSON замовлення
curl -i --cacert certs/generated/rootCA.crt \
  --cert-type P12 --cert certs/generated/client-keystore.p12:changeit \
  https://localhost:8444/api/mtls/orders/ORD-2001
```

## Що тут відпрацьовується (теорія -> практика)

| Концепція | Де в скрипті / коді |
|---|---|
| Ланцюг довіри (chain of trust), self-signed root CA | `openssl req -x509 ...` |
| CSR -> підписаний сертифікат | `openssl req -new` + `openssl x509 -req -CA ...` |
| SAN (Subject Alternative Name) для валідації hostname | `server-ext.cnf` (`subjectAltName`) |
| extendedKeyUsage: serverAuth / clientAuth | `*-ext.cnf` |
| Keystore (ідентичність) vs Truststore (кому довіряти) | `server-keystore.p12` vs `server-truststore.p12` |
| mTLS handshake, client-auth=want | `MtlsConnectorConfig` у rest-service |
| Мапінг CN сертифіката -> принципал/роль | `SecurityConfig.mtlsChain` у rest-service |

Файли в `certs/generated/` містять приватні ключі — вони **не комітяться**
(`.gitignore`), генеруються локально кожним розробником окремо.
