package com.sleekydz86.catalog.global.security;

public final class CdwRole {

    public static final String CDW_ADMIN = "ROLE_CDW_ADMIN";
    public static final String MIGRATION_OPERATOR = "ROLE_MIGRATION_OPERATOR";
    public static final String EXTRACT_OPERATOR = "ROLE_EXTRACT_OPERATOR";
    public static final String METADATA_READER = "ROLE_METADATA_READER";
    public static final String WORKER = "ROLE_WORKER";

    private CdwRole() {
    }

    public static String toAuthority(String role) {
        if (role == null || role.isBlank()) {
            return role;
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            return normalized;
        }
        return "ROLE_" + normalized;
    }
}
