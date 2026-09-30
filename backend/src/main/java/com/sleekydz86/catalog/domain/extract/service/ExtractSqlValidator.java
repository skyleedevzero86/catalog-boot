package com.sleekydz86.catalog.domain.extract.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class ExtractSqlValidator {

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "MERGE", "DROP", "ALTER", "TRUNCATE",
            "CREATE", "GRANT", "REVOKE", "CALL", "EXEC", "EXECUTE", "COPY", "INTO", "FOR"
    );

    private static final Pattern MULTI_STATEMENT = Pattern.compile(";\\s*\\S");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*");
    private static final Pattern LINE_COMMENT = Pattern.compile("--");

    public void validateReadOnlySelect(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL은 비어 있을 수 없습니다.");
        }
        String normalized = sql.trim();
        if (MULTI_STATEMENT.matcher(normalized).find()) {
            throw new IllegalArgumentException("다중 SQL 문은 허용되지 않습니다.");
        }
        if (BLOCK_COMMENT.matcher(normalized).find() || LINE_COMMENT.matcher(normalized).find()) {
            throw new IllegalArgumentException("SQL 주석은 허용되지 않습니다.");
        }
        String compact = normalized.replaceAll("\\s+", " ").trim();
        if (!compact.regionMatches(true, 0, "SELECT", 0, 6)) {
            throw new IllegalArgumentException("SELECT 문만 허용됩니다.");
        }
        String upper = compact.toUpperCase(Locale.ROOT);
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (containsKeyword(upper, keyword)) {
                throw new IllegalArgumentException("허용되지 않는 SQL 키워드가 포함되어 있습니다.");
            }
        }
        if (upper.contains(" UNION ") || upper.contains(" UNION\n") || upper.endsWith(" UNION")) {
            throw new IllegalArgumentException("UNION은 허용되지 않습니다.");
        }
    }

    private boolean containsKeyword(String upperSql, String keyword) {
        int index = upperSql.indexOf(keyword);
        while (index >= 0) {
            boolean startOk = index == 0 || !Character.isLetterOrDigit(upperSql.charAt(index - 1));
            int endIndex = index + keyword.length();
            boolean endOk = endIndex >= upperSql.length() || !Character.isLetterOrDigit(upperSql.charAt(endIndex));
            if (startOk && endOk) {
                return true;
            }
            index = upperSql.indexOf(keyword, index + 1);
        }
        return false;
    }
}
