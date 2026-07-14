package com.usang.marketdata.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

// RAG 뉴스 코퍼스(news_article)는 pgvector(Postgres)에, 나머지 엔티티는 기존 MySQL에 저장하는 이원화 구성.
// MySQL을 명시적으로 재선언해 @Primary로 지정해야 JPA/JdbcTemplate 자동 설정이 어떤 DataSource를
// 써야 할지 모호해지지 않는다 (DataSource 빈이 2개가 되므로).
// DataSourceProperties.initializeDataSourceBuilder()를 거쳐야 Hikari의 jdbcUrl 등으로 올바르게
// 매핑된다 — @ConfigurationProperties를 DataSource 빈에 직접 바인딩하면 프로퍼티명이 어긋난다.
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource mysqlDataSource(@Qualifier("mysqlDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("app.postgres")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource vectorDataSource(@Qualifier("vectorDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }
}
