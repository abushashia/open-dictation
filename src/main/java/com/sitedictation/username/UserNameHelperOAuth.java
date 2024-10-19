package com.sitedictation.username;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Principal;
import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "dictation", name = "oauth2-enabled", havingValue = "true")
class UserNameHelperOAuth implements UserNameHelper {

    @Value("${dictation.admin-user-name}")
    private String adminUserName;

    @PostConstruct
    private void validate() {
        Objects.requireNonNull(adminUserName);
    }

    @Override
    public String getUserName(Principal principal) {
        if (principal == null) {
            return null;
        }
        String userName = principal.getName();
        if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            OAuth2User oAuth2User = oAuth2AuthenticationToken.getPrincipal();
            userName = oAuth2User.getAttribute("email");
        }
        return userName;
    }

    @Override
    public boolean isAdmin(Principal principal) {
        return Objects.equals(getUserName(principal), adminUserName);
    }

    @Override
    public boolean isAdmin(String email) {
        return Objects.equals(email, adminUserName);
    }
}
