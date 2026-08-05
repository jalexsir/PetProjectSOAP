# PetProjectSOAP — Integration Developer Practice Lab

Pet-проєкт на Java/Spring Boot/Apache Camel, який на практиці демонструє
**кожен пункт** вимог із вакансії "Розробник інтеграцій": SOAP, REST, MQTT,
XML/XSD/WSDL, JSON/JSON Schema, Basic/mTLS/OAuth2, адміністрування
TLS-сертифікатів, Cross Domain Solution, ESB/SOA (Apache Camel).

Теорія (для підготовки до співбесіди, пункт за пунктом) — у
**[docs/THEORY.md](docs/THEORY.md)** та **[docs/SAP-PI-PO-THEORY.md](docs/SAP-PI-PO-THEORY.md)**.

## Архітектура

```
                         ┌──────────────────────┐
                         │   integration-hub     │   "міні-ESB" (Apache Camel)
                         │  CDS-guard -> Router   │   POST /hub/orders
                         └──────────┬─────┬──────┘
                    translate JSON->XML │  │
                         ┌────────────┘   └────────────┐
                         ▼                              ▼
              ┌────────────────────┐        ┌────────────────────┐       ┌─────────────────────┐
              │    soap-service     │        │    rest-service     │       │   mqtt-integration   │
              │  Spring-WS (SOAP)   │        │  Spring Boot (REST) │       │  Paho + Moquette      │
              │  Basic Auth         │        │  Basic/OAuth2/mTLS  │       │  pub/sub, QoS 1       │
              │  :8081/ws           │        │  :8082 (+:8444 mTLS)│       │  tcp://:1883          │
              └────────────────────┘        └────────────────────┘       └─────────────────────┘
                         ▲                              ▲                            ▲
                         └──────────────────────────────┴────────────────────────────┘
                                    спільна канонічна модель (common-model):
                                    order.xsd + order-schema.json + JAXB + OrderMapper
```

## Модулі

| Модуль | Що демонструє |
|---|---|
| `common-model` | Канонічна модель `Order`: XSD, JSON Schema, JAXB-класи, `OrderMapper` (XML ↔ JSON), `JsonSchemaValidator` |
| `soap-service` | Contract-first SOAP (Spring-WS): WSDL генерується з XSD, SOAP Fault, HTTP Basic |
| `rest-service` | REST + JSON Schema валідація; три паралельні механізми автентифікації: Basic / OAuth2 (JWT) / mTLS |
| `mqtt-integration` | MQTT pub/sub (Eclipse Paho) + вбудований брокер (Moquette) |
| `integration-hub` | "Міні-ESB" на Apache Camel: Cross-Domain-Solution guard, Content-Based Router, Message Translator, Channel Adapters до SOAP/REST/MQTT |
| `certs/` | Скрипт генерації TLS-сертифікатів (root CA, server, client) для mTLS-демо |

## Вимоги

- Java 21, Maven 3.9+
- (опційно) Docker — для зовнішнього MQTT-брокера (Mosquitto) замість вбудованого

## Збірка

```bash
mvn clean install
```

## Запуск

Кожен сервіс — окремий Spring Boot застосунок, запускається незалежно:

```bash
mvn -pl soap-service spring-boot:run        # :8081 — SOAP (WSDL: /ws/orders.wsdl)
mvn -pl rest-service spring-boot:run        # :8082 — REST (Basic/OAuth2), mTLS вимкнено за замовчуванням
mvn -pl mqtt-integration spring-boot:run    # embedded MQTT broker :1883 + демо publish/subscribe
mvn -pl integration-hub spring-boot:run     # :8083 — ESB (потребує запущених soap/rest/mqtt для реальної маршрутизації)
```

### mTLS-демо (rest-service)

```bash
./certs/generate-certs.sh
MTLS_ENABLED=true mvn -pl rest-service spring-boot:run   # додатково піднімає :8444 з client-auth=want
```

Детальніше — [certs/README.md](certs/README.md).

## Швидкі перевірки по кожній вимозі вакансії

### SOAP + XML/XSD/WSDL + Basic Auth

```bash
curl http://localhost:8081/ws/orders.wsdl

curl -u integration:integration-pass -H "Content-Type: text/xml" \
  -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
        <getOrderRequest xmlns="http://petproject.integration/orders"><orderId>ORD-1001</orderId></getOrderRequest>
      </soap:Body></soap:Envelope>' \
  http://localhost:8081/ws
```

### REST + JSON Schema + Basic Auth

```bash
curl -u integration:integration-pass http://localhost:8082/api/basic/orders/ORD-2001

# 400 — порушення JSON Schema (немає обов'язкового поля "items")
curl -u integration:integration-pass -H "Content-Type: application/json" \
  -d '{"orderId":"x","customerId":"c","status":"NEW","createdAt":"2026-08-05T10:00:00Z"}' \
  http://localhost:8082/api/basic/orders
```

### OAuth2 / JWT

```bash
TOKEN=$(curl -s -X POST "http://localhost:8082/api/public/auth/token?subject=demo&scope=orders.read" | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/oauth/orders/ORD-2001
```

### mTLS

```bash
curl --cacert certs/generated/rootCA.crt \
  --cert-type P12 --cert certs/generated/client-keystore.p12:changeit \
  https://localhost:8444/api/mtls/orders/ORD-2001
```

### MQTT

`mvn -pl mqtt-integration spring-boot:run` сам публікує та одразу консьюмить
демо-подію при старті — дивись лог `Downstream consumer received event ...`.
Тести: `mvn -pl mqtt-integration test`.

### ESB / CDS (integration-hub)

```bash
curl -X POST http://localhost:8083/hub/orders \
  -H "Content-Type: application/json" -H "X-Channel: soap" \
  -d '{"orderId":"ignored","customerId":"CUST-1","status":"NEW","createdAt":"2026-08-05T10:00:00Z","items":[{"sku":"SKU-1","quantity":1,"unitPrice":9.99}]}'
```

Автоматичні тести маршрутів (без потреби піднімати всі сервіси) —
`mvn -pl integration-hub test`: перевіряють Content-Based Router, JSON→XML
Message Translator і відхилення невалідного повідомлення CDS-guard-ом.

## Мапінг вимог вакансії на код

| Вимога | Модуль / файл |
|---|---|
| SOAP, REST, MQTT | `soap-service`, `rest-service`, `mqtt-integration` |
| XML, XSD, WSDL, JSON, JSON Schema | `common-model/src/main/resources/{xsd,json-schema}`, `soap-service/.../WebServiceConfig` |
| Basic, mTLS, OAuth2 | `SoapSecurityConfig`, `rest-service/.../SecurityConfig`, `MtlsConnectorConfig`, `AuthTokenController` |
| Адміністрування TLS/SSL сертифікатів | `certs/generate-certs.sh`, `certs/README.md` |
| Cross Domain Solution | `integration-hub/.../CrossDomainGuardProcessor` |
| ESB / SOA | `integration-hub/.../IntegrationHubRoutes` (Apache Camel) |
| SAP PI/PO, SAP NetWeaver AS Java (бажано) | теорія + мапінг понять — `docs/SAP-PI-PO-THEORY.md` |
| Java EE Application Servers (бажано) | теорія + приклад JAX-WS коду — `docs/THEORY.md` розділ 8 |

## Тести

```bash
mvn test                       # усі модулі
mvn -pl rest-service test      # Basic/OAuth2/JSON Schema сценарії
mvn -pl soap-service test      # SOAP success + SOAP Fault сценарії
mvn -pl mqtt-integration test  # publish -> subscribe round-trip
mvn -pl integration-hub test   # CBR + translator + CDS-guard
```
