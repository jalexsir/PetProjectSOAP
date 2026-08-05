package com.petproject.integration.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Чотири незалежні SecurityFilterChain — по одному на кожен сегмент API,
 * кожен зі своїм механізмом автентифікації. Це навмисно "паралельна" схема
 * (а не одна конфігурація "на все"), щоб було видно різницю між Basic, OAuth2/JWT
 * та mTLS(X.509) в одному застосунку.
 */
@Configuration
public class SecurityConfig {

    // /api/public/**, /.well-known/** — без автентифікації (видача токена, JWKS, health)
    @Bean
    @Order(1)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/public/**", "/.well-known/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // /api/basic/** — HTTP Basic (RFC 7617): Authorization: Basic base64(user:pass)
    @Bean
    @Order(2)
    public SecurityFilterChain basicChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/basic/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    // /api/oauth/** — OAuth2 Resource Server: Authorization: Bearer <JWT>, підпис перевіряється через JWKS
    @Bean
    @Order(3)
    public SecurityFilterChain oauthChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/oauth/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/oauth/**").hasAuthority("SCOPE_orders.read")
                        .requestMatchers(HttpMethod.POST, "/api/oauth/**").hasAuthority("SCOPE_orders.write")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    // /api/mtls/** — клієнт автентифікується власним X.509-сертифікатом під час TLS-handshake
    // (дивись MtlsConnectorConfig: додатковий Tomcat-конектор із client-auth=want)
    @Bean
    @Order(4)
    public SecurityFilterChain mtlsChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/mtls/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .x509(x509 -> x509
                        .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                        .userDetailsService(x509UserDetailsService()));
        return http.build();
    }

    @Bean
    public UserDetailsService basicAuthUserDetailsService() {
        UserDetails integrationUser = User.withUsername("integration")
                .password("{noop}integration-pass")
                .roles("INTEGRATION")
                .build();
        return new InMemoryUserDetailsManager(integrationUser);
    }

    // Принципал видобувається з CN сертифіката (наприклад CN=integration-client),
    // будь-який успішно валідований (довіреним CA) сертифікат отримує роль клієнта інтеграції.
    private UserDetailsService x509UserDetailsService() {
        return commonName -> User.withUsername(commonName)
                .password("")
                .authorities("ROLE_MTLS_CLIENT")
                .build();
    }
}
