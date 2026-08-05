package com.petproject.integration.soap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP Basic Authentication на SOAP-каналі (RFC 7617): облікові дані передаються
 * в заголовку Authorization: Basic base64(user:pass) на КОЖЕН запит.
 * Це один із трьох механізмів автентифікації з вимог вакансії —
 * REST-сервіс (rest-service) додатково показує mTLS та OAuth2.
 *
 * ВАЖЛИВО: {noop} та пароль у коді — лише для навчального прикладу.
 * У проді паролі зберігаються хешованими (BCrypt) і в секрет-сховищі, не в коді.
 */
@Configuration
public class SoapSecurityConfig {

    @Bean
    public SecurityFilterChain soapSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/orders.wsdl").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager soapUserDetailsService() {
        UserDetails integrationUser = User.withUsername("integration")
                .password("{noop}integration-pass")
                .roles("INTEGRATION")
                .build();
        return new InMemoryUserDetailsManager(integrationUser);
    }
}
