package com.petproject.integration.mqtt.broker;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.Properties;

/**
 * Вбудований MQTT-брокер (Moquette) — щоб pet-проєкт демонстрував pub/sub
 * "з коробки" без потреби піднімати окрему інфраструктуру. У реальній системі
 * брокером був би окремий кластер (Mosquitto/EMQX/HiveMQ) — для цього в репо
 * є docker-compose.yml з Mosquitto як альтернатива (mqtt.embedded-broker.enabled=false).
 */
@Component
public class EmbeddedMqttBroker {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedMqttBroker.class);

    @Value("${mqtt.embedded-broker.enabled:true}")
    private boolean enabled;

    @Value("${mqtt.broker.port:1883}")
    private int port;

    private Server server;

    @PostConstruct
    public void start() throws Exception {
        if (!enabled) {
            LOGGER.info("Embedded MQTT broker disabled — очікується зовнішній брокер " +
                    "(наприклад, Mosquitto з docker-compose.yml).");
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("port", String.valueOf(port));
        properties.setProperty("host", "0.0.0.0");
        properties.setProperty("allow_anonymous", "true");
        properties.setProperty("persistent_store", Files.createTempFile("moquette-store", ".mapdb").toString());

        IConfig config = new MemoryConfig(properties);
        server = new Server();
        server.startServer(config);
        LOGGER.info("Embedded MQTT broker (Moquette) started on tcp://0.0.0.0:{}", port);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.stopServer();
            LOGGER.info("Embedded MQTT broker stopped");
        }
    }
}
