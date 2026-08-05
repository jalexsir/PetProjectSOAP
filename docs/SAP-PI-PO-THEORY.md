# SAP PI/PO та SAP NetWeaver AS Java — теорія (бажані навички з вакансії)

Це власницькі (proprietary) технології SAP: ліцензія й повноцінне середовище
недоступні для pet-проєкту, тому тут — **лише теорія й прямий мапінг понять**
на те, що вже реалізовано в цьому репозиторії на Apache Camel/Spring-WS.
Мета — щоб на співбесіді можна було впевнено пояснити концепції SAP PI/PO,
навіть без "живого" SAP-ландшафту під рукою, спираючись на аналогії з
реально написаним кодом.

## Що таке SAP PI/PO

- **SAP PI (Process Integration)**, пізніше перейменований на **SAP PO
  (Process Orchestration)** — це **ESB-рішення SAP-екосистеми** (розділ 6
  [THEORY.md](./THEORY.md) — "ESB на практиці, тільки виробництва SAP").
- PO = PI + доданий **BPM (Business Process Management)** та **BRM (Business
  Rules Management)** — тобто оркестрація довгих бізнес-процесів і
  декларативні бізнес-правила поверх звичайної маршрутизації повідомлень.
- Архітектурно складається з:
  - **Integration Builder** — інструмент розробки/конфігурування;
  - **Integration Repository (IR)** — дизайн-тайм: структури повідомлень
    (аналог наших XSD), інтерфейси (Service Interfaces), мапінги;
  - **Integration Directory (ID)** — конфігурація-тайм: канали зв'язку
    (Communication Channels), конкретні маршрути (ICO — Integrated
    Configuration Objects) між конкретними системами;
  - **Advanced Adapter Engine (AAE/AEX)** — виконує реальну доставку через
    адаптери (SOAP, IDoc, RFC, JDBC, File, JMS, HTTP, mail тощо).

## Мапінг понять SAP PI/PO ↔ цей проєкт (Apache Camel)

| SAP PI/PO | Роль | Аналог у `integration-hub` |
|---|---|---|
| Message Type / Data Type (Integration Repository) | Контракт структури повідомлення | `order.xsd` / `order-schema.json` (`common-model`) |
| Service Interface (Inbound/Outbound) | Опис "хто що приймає/відправляє" | `SoapChannelGateway` / `RestChannelGateway` / `MqttChannelGateway` (інтерфейси) |
| Communication Channel | Технічні параметри конкретного з'єднання (адреса, автентифікація) | `HttpSoapChannelGateway` / `HttpRestChannelGateway` / `PahoMqttChannelGateway` (реалізації, конфігуруються через `application.yml`) |
| Message Mapping (Graphical/XSLT/Java) | Перетворення структури A → структури B | `JsonToXmlOrderTranslator` (Message Translator EIP) |
| Integrated Configuration Object (ICO) | Конкретний маршрут: sender + receiver(s) + мапінг + канали | Camel-маршрути в `IntegrationHubRoutes` (`route-to-soap`, `route-to-rest`, `route-to-mqtt`) |
| Receiver Determination / Interface Determination | "За яких умов повідомлення йде до якого отримувача" | `choice()/when(header("channel")...)` — Content-Based Router |
| ccBPM / BPM (оркестрація процесу з кількома кроками) | Довгий бізнес-процес поверх кількох повідомлень | У проєкті не реалізовано (поза обсягом pet-проєкту), але саме тут використовувався б Camel `saga`/`process` EIP або окремий оркестратор |
| Value Mapping | Довідники відповідності кодів між системами (напр. код країни SAP ↔ код країни партнера) | Можна було б додати як окремий `Processor`, що підміняє значення полів за довідником — у проєкті спрощено до прямого мапінгу |
| Adapter Engine (AAE) моніторинг (SXMB_MONI аналог — тепер PIMon/AEX) | Трасування кожного повідомлення через систему | `CDS_AUDIT` structured-логування в `CrossDomainGuardProcessor` + INFO-логи на кожному кроці маршруту |

## SAP NetWeaver Application Server Java (AS Java)

- Java EE-подібний **application server** від SAP (частина NetWeaver
  платформи) — той самий клас систем, що WildFly/WebSphere/WebLogic (розділ 8
  [THEORY.md](./THEORY.md)).
- SAP PI/PO у класичній ("double-stack") конфігурації розгортався саме на
  **AS Java**: Integration Engine і Adapter Engine — це Java EE-застосунки
  (J2EE-компоненти), що виконувались у NetWeaver-контейнері.
- **Адміністрування AS Java** типово включає:
  - керування інстансами/кластером (Instance Agent, SAP MMC / SAP Management
    Console);
  - розгортання (deployment) SDA/SCA-архівів (SAP-специфічні пакування,
    концептуально аналогічні WAR/EAR у "ванільному" Java EE);
  - керування User Management Engine (UME) — джерела користувачів/ролей,
    інтеграція з LDAP/ABAP-бекендом;
  - конфігурацію JCo (Java Connector) — RFC-з'єднання до ABAP-стеку;
  - моніторинг через NetWeaver Administrator (NWA) — стан кластера, JVM-пам'ять,
    thread dumps — ті самі задачі, що адміністратор будь-якого Java
    application server виконує на WildFly/WebLogic, лише через SAP-специфічний
    UI.
- **Чому це релевантно для "Розробника інтеграцій":** навіть не адмініструючи
  AS Java щодня, важливо розуміти, що PI/PO — це **звичайний Java-застосунок
  у контейнері**, а не "чорна скринька" — тому діагностика (thread dump,
  heap dump, лог-файли `defaultTrace.trc`) відбувається тими самими методами,
  що й для будь-якого Java EE app server.

## IDoc і RFC — типові "сусіди" PI/PO, яких немає в pet-проєкті

- **IDoc (Intermediate Document)** — пропрієтарний ASCII/XML-подібний формат
  SAP для пакетного обміну бізнес-документами (замовлення, накладні тощо) з
  ABAP-стеком. Концептуально це **ще один канонічний формат повідомлення**,
  так само як XML/JSON у цьому проєкті — просто SAP-специфічний.
- **RFC (Remote Function Call)** — SAP-протокол виклику функцій в ABAP-системі
  (аналог RPC). У Java-світі викликається через **JCo (SAP Java Connector)**.
- В інтерв'ю корисно вміти сказати: *"У проєкті IDoc/RFC не було сенсу
  реалізовувати (пропрієтарний протокол, потребує ABAP-бекенду), але
  архітектурно вони відіграють ту саму роль, що й наш SOAP/REST/MQTT-канал —
  просто ще один Channel Adapter, підключений до того самого Content-Based
  Router у ESB."*

## Типові питання на співбесіді по цьому блоку

- *"Чим PI відрізняється від PO?"* → PO = PI + BPM/BRM (оркестрація процесів
  і бізнес-правила поверх базової маршрутизації повідомлень).
- *"Що таке ICO?"* → Integrated Configuration Object — об'єкт в Integration
  Directory, що зв'язує sender/receiver, канали зв'язку й мапінг в один
  керований маршрут (у нашому проєкті — аналог одного Camel-маршруту в
  `IntegrationHubRoutes`).
- *"Як би ти діагностував, чому повідомлення "застрягло" в PI/PO?"* →
  моніторинг черги повідомлень (аналог перевірки логів
  `route-to-soap`/`route-to-rest`/`route-to-mqtt` у нашому `integration-hub`),
  перевірка Communication Channel (доступність цільової системи — аналог
  перевірки `hub.soap-service.base-url` у `application.yml`), перевірка
  мапінгу на помилки трансформації (аналог `JsonToXmlOrderTranslator`).
