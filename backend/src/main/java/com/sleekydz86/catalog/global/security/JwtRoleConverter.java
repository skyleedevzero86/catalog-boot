package com.sleekydz86.catalog.global.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        appendRoles(roles, jwt.getClaimAsStringList("roles"));
        appendRoles(roles, jwt.getClaimAsStringList("authorities"));
        Object scope = jwt.getClaim("scope");
        if (scope instanceof String scopeString) {
            for (String value : scopeString.split(" ")) {
                appendRole(roles, value);
            }
        } else if (scope instanceof Collection<?> scopeCollection) {
            for (Object value : scopeCollection) {
                appendRole(roles, value == null ? null : value.toString());
            }
        }
        return roles.stream().map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
    }

    private void appendRoles(Set<String> roles, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            appendRole(roles, value);
        }
    }

    private void appendRole(Set<String> roles, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        roles.add(CdwRole.toAuthority(value));
    }
}
