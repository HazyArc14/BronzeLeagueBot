package com.hazyarc14.database;

import com.zaxxer.hikari.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${SPRING_DATASOURCE_URL}")
    private String datasourceUrl;

    @Value("${SPRING_DATASOURCE_USERNAME}")
    private String datasourceUsername;

    @Value("${SPRING_DATASOURCE_PASSWORD}")
    private String datasourcePassword;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(datasourceUrl);
        config.setUsername(datasourceUsername);
        config.setPassword(datasourcePassword);
        return new HikariDataSource(config);
    }

}