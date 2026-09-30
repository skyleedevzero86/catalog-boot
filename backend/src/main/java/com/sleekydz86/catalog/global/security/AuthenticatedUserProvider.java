package com.sleekydz86.catalog.global.security;

import java.util.Set;

public interface AuthenticatedUserProvider {

    String currentUserId();

    Set<String> currentRoles();
}
