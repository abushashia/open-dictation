package com.sitedictation.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(prefix = "dictation", name = "oauth2-enabled", havingValue = "true")
class SecurityConfig {

    @Value("${dictation.admin-user-name}")
    private String adminUserName;

    @PostConstruct
    private void validate() {
        Objects.requireNonNull(adminUserName);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authz) ->
                        authz.requestMatchers("/actuator/health").hasRole("ADMIN")
                                .requestMatchers("/actuator/info").hasRole("ADMIN")
                                .requestMatchers("/actuator/metrics").hasRole("ADMIN")
                                .requestMatchers("/actuator/prometheus").hasRole("ADMIN")
                                .requestMatchers("favicon.ico").permitAll()
                                .requestMatchers("/images/**").permitAll()
                                .requestMatchers("/landing/**").permitAll()
                                .requestMatchers("/terms").permitAll()
                                .requestMatchers("/privacy").permitAll()
                                .anyRequest().authenticated());
        http.oauth2Login(oauth2 -> oauth2.userInfoEndpoint(
                userInfo -> userInfo.userAuthoritiesMapper(userAuthoritiesMapper())));
        return http.build();
    }

    // https://stackoverflow.com/questions/63875298/spring-security-5-providing-roles-for-oauth2-authenticated-users
    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            if (isAdmin(authorities, adminUserName)) {
                Set<GrantedAuthority> mappedAuthorities = new HashSet<>(authorities);
                mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                return mappedAuthorities;
            }
            return authorities;
        };
    }

    private static boolean isAdmin(Collection<? extends GrantedAuthority> authorities, String adminUserName) {
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority) {
                OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;
                OidcIdToken idToken = oidcUserAuthority.getIdToken();
                String email = idToken.getEmail();
                Boolean emailVerified = idToken.getEmailVerified();
                if (Objects.equals(true, emailVerified)
                        && Objects.nonNull(email)
                        && Objects.equals(email, adminUserName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
