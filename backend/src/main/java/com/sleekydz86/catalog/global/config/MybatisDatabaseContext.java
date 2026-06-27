package com.sleekydz86.catalog.global.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;

@Component
public class MybatisDatabaseContext {

    private final String databaseId;

    public MybatisDatabaseContext(SqlSessionFactory sqlSessionFactory) {
        this.databaseId = sqlSessionFactory.getConfiguration().getDatabaseId();
    }

    public boolean isPostgreSQL() {
        return "PostgreSQL".equals(databaseId);
    }
}
