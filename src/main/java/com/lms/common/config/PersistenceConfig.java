package com.lms.common.config;

import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Two Hibernate settings the schema needs. They live here rather than in
 * application.yml so the connection settings there stay untouched.
 */
@Configuration
public class PersistenceConfig {

    /**
     * Use table and column names exactly as written in {@code @Table} and
     * {@code @Column}.
     *
     * <p>Spring Boot's default naming strategy rewrites every name to
     * snake_case — even explicit ones — so {@code @Table(name = "AppUser")}
     * would be looked up as {@code app_user}, and schema validation would
     * report every table missing. 01_schema.sql is PascalCase (CLAUDE.md
     * rule 8), so names must pass through unchanged. Defining this bean is
     * Spring Boot's documented way to replace the default.
     */
    @Bean
    public PhysicalNamingStrategy physicalNamingStrategy() {
        return new PhysicalNamingStrategyStandardImpl();
    }

    /**
     * Treat every string column as national (Unicode) character data.
     *
     * <p>Every character column in 01_schema.sql is NVARCHAR, so member and
     * author names in Sinhala or Tamil are stored intact. Without this,
     * Hibernate models Java strings as VARCHAR and binds parameters as
     * non-Unicode.
     */
    @Bean
    public HibernatePropertiesCustomizer nationalizedCharacterData() {
        return properties -> properties.put(AvailableSettings.USE_NATIONALIZED_CHARACTER_DATA, true);
    }
}
