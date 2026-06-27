package com.sleekydz86.catalog.global.config;


import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@MapperScan(basePackages = {
        "com.sleekydz86.adapter.outbound.persistence.connection",
        "com.sleekydz86.adapter.outbound.persistence.migration",
        "com.sleekydz86.adapter.outbound.persistence.codetype",
        "com.sleekydz86.adapter.outbound.persistence.metadata",
        "com.sleekydz86.adapter.outbound.persistence.category",
        "com.sleekydz86.adapter.outbound.persistence.extract"
})
public class MybatisConfig {

    @Bean
    VendorDatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("PostgreSQL", "PostgreSQL");
        properties.setProperty("H2", "H2");
        provider.setProperties(properties);
        return provider;
    }
}
