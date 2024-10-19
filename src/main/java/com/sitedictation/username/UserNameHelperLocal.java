package com.sitedictation.username;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Principal;
import java.util.Objects;

@Component
@Primary
@ConditionalOnMissingBean(UserNameHelperOAuth.class)
class UserNameHelperLocal implements UserNameHelper {

    @Value("${dictation.admin-user-name}")
    private String adminUserName;

    @PostConstruct
    private void validate() {
        Objects.requireNonNull(adminUserName);
    }

    @Override
    public String getUserName(Principal principal) {
        return adminUserName;
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
