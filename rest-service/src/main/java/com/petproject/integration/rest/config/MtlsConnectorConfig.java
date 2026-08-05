package com.petproject.integration.rest.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Додає ДРУГИЙ, окремий HTTPS-конектор Tomcat спеціально для mTLS-демо
 * (/api/mtls/**), не чіпаючи основний HTTP-порт, на якому й далі працюють
 * /api/basic/** та /api/oauth/**. Це найточніше відтворює те, як TLS/mTLS
 * адмініструється на реальному application-сервері: окремий listener/порт
 * зі своїм keystore (ідентичність сервера) та truststore (яким клієнтським
 * сертифікатам довіряти), clientAuth=WANT — сертифікат запитується, але
 * не є обов'язковим на рівні TLS-handshake (обов'язковість на рівні шляху
 * забезпечує Spring Security, дивись SecurityConfig.mtlsChain).
 *
 * Keystore/truststore генеруються скриптом certs/generate-certs.sh.
 * Якщо файлів немає — конектор просто не піднімається (застосунок стартує
 * нормально, basic/oauth demo працює "з коробки" без обов'язкової генерації сертифікатів).
 */
@Configuration
public class MtlsConnectorConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtlsConnectorConfig.class);

    @Value("${mtls.enabled:false}")
    private boolean mtlsEnabled;

    @Value("${mtls.port:8444}")
    private int mtlsPort;

    @Value("${mtls.keystore:../certs/generated/server-keystore.p12}")
    private String keystorePath;

    @Value("${mtls.keystore-password:changeit}")
    private String keystorePassword;

    @Value("${mtls.truststore:../certs/generated/server-truststore.p12}")
    private String truststorePath;

    @Value("${mtls.truststore-password:changeit}")
    private String truststorePassword;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> mtlsConnectorCustomizer() {
        return factory -> {
            if (!mtlsEnabled) {
                LOGGER.info("mTLS connector disabled (mtls.enabled=false). /api/mtls/** will reject requests " +
                        "on the plain HTTP port (no client certificate available there).");
                return;
            }
            File keystoreFile = new File(keystorePath);
            File truststoreFile = new File(truststorePath);
            if (!keystoreFile.exists() || !truststoreFile.exists()) {
                LOGGER.warn("mTLS connector requested but keystore/truststore not found ({} / {}). " +
                        "Run certs/generate-certs.sh first. Skipping mTLS connector.", keystorePath, truststorePath);
                return;
            }

            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(mtlsPort);
            connector.setScheme("https");
            connector.setSecure(true);

            Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
            protocol.setSSLEnabled(true);

            SSLHostConfig sslHostConfig = new SSLHostConfig();
            SSLHostConfigCertificate certificate =
                    new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.RSA);
            certificate.setCertificateKeystoreFile(keystoreFile.getAbsolutePath());
            certificate.setCertificateKeystorePassword(keystorePassword);
            certificate.setCertificateKeystoreType("PKCS12");
            sslHostConfig.addCertificate(certificate);

            sslHostConfig.setTruststoreFile(truststoreFile.getAbsolutePath());
            sslHostConfig.setTruststorePassword(truststorePassword);
            // WANT: сервер запитує клієнтський сертифікат, але не рве TLS-handshake за його відсутності;
            // обов'язковість для конкретних шляхів реалізує Spring Security (x509 filter chain).
            sslHostConfig.setCertificateVerification("optional");

            connector.addSslHostConfig(sslHostConfig);
            factory.addAdditionalTomcatConnectors(connector);

            LOGGER.info("mTLS connector started on port {} (keystore={}, truststore={})",
                    mtlsPort, keystorePath, truststorePath);
        };
    }
}
