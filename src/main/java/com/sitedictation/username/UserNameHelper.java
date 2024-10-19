package com.sitedictation.username;

import java.security.Principal;

public interface UserNameHelper {

    String getUserName(Principal principal);

    boolean isAdmin(Principal principal);

    boolean isAdmin(String email);
}
