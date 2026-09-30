package com.sleekydz86.catalog.global.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class WorkerTokenAuthentication extends AbstractAuthenticationToken {

    private final String principal;

    public WorkerTokenAuthentication(String principal) {
        super(List.of(new SimpleGrantedAuthority(CdwRole.WORKER)));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
