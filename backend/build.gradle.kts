plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.sleekydz86"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation ("org.springframework.boot:spring-boot-starter-actuator")
    implementation ("org.springframework.boot:spring-boot-starter-security")
    implementation ("org.springframework.boot:spring-boot-starter-validation")
    implementation ("org.springframework.boot:spring-boot-starter-web")
    implementation ("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.5")
    implementation ("org.flywaydb:flyway-core")
    implementation ("org.flywaydb:flyway-database-postgresql")
    implementation ("org.springframework.boot:spring-boot-starter-log4j2")
    implementation ("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation ("org.apache.parquet:parquet-avro:1.14.3")
    implementation ("org.apache.hadoop:hadoop-common:3.4.1")
    compileOnly ("org.projectlombok:lombok")
    runtimeOnly ("com.mysql:mysql-connector-j")
    runtimeOnly ("org.mariadb.jdbc:mariadb-java-client")
    runtimeOnly ("org.postgresql:postgresql")
    runtimeOnly ("com.oracle.database.jdbc:ojdbc11:23.26.2.0.0")
    runtimeOnly ("com.clickhouse:clickhouse-jdbc:0.9.8")
    annotationProcessor ("org.projectlombok:lombok")
    testImplementation ("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly ("com.h2database:h2")
    testRuntimeOnly ("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
