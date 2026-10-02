package com.sleekydz86.catalog.global.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class SecurityAuthenticatedUserProvider implements AuthenticatedUserProvider {

    @Override
    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwt) {
                String subject = jwt.getSubject();
                if (subject != null && !subject.isBlank()) {
                    return subject.trim();
                }
                Object userId = jwt.getClaim("userId");
                if (userId != null && !userId.toString().isBlank()) {
                    return userId.toString().trim();
                }
            }
            String name = authentication.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equals(name)) {
                return name.trim();
            }
        }
        return userIdFromHeader().orElse("system");
    }

    @Override
    public Set<String> currentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            roles.add(authority.getAuthority());
        }
        return Set.copyOf(roles);
    }

    private Optional<String> userIdFromHeader() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return Optional.empty();
        }
        String header = servletAttributes.getRequest().getHeader("userId");
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(header.trim());
    }
}
