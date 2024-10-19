package com.sitedictation.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConditionalOnMissingBean(SecurityConfig.class)
class SecurityConfigNoSecurity {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll());

        // Due to "Cannot create a session after the response has been committed"
        // http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
        // Alternatively,
        http.csrf(AbstractHttpConfigurer::disable);
        // See https://stackoverflow.com/questions/52982246/spring-thymeleaf-throws-cannot-create-a-session-after-the-response-has-been-com
        return http.build();
    }
}
