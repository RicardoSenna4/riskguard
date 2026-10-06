package com.riskguard.transaction;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
@Testcontainers @EnabledIfEnvironmentVariable(named="RUN_CONTAINERS",matches="true") class PostgresContainerSmokeTest { @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine"); @Test void startsSupportedPostgres(){assertThat(postgres.isRunning()).isTrue();assertThat(postgres.getJdbcUrl()).startsWith("jdbc:postgresql:");} }
