package io.quarkus.liquibase.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.liquibase.common.LiquibaseChangeLogResourceDiscovery;
import io.quarkus.liquibase.common.LiquibaseChangeLogResourceDiscovery.LogicalPhysicalAlias;
import io.quarkus.liquibase.common.LiquibaseClasspathResources;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;

class LiquibaseChangeLogResourceDiscoveryTest {

    @Test
    void logicalFilePathProducesAliasForNativeEmbedding() throws Exception {
        try (ClassLoaderResourceAccessor accessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader())) {

            ChangeLogParserFactory factory = ChangeLogParserFactory.getInstance();
            ChangeLogParser parser = factory.getParser("discovery/logical-path-root.xml", accessor);
            DatabaseChangeLog changelog = parser.parse("discovery/logical-path-root.xml", new ChangeLogParameters(),
                    accessor);

            LiquibaseChangeLogResourceDiscovery.ScanResult scan = LiquibaseChangeLogResourceDiscovery.scan(changelog);

            assertThat(scan.logicalPhysicalAliases())
                    .extracting(LogicalPhysicalAlias::logical)
                    .contains("db/changelog/db.changelog-1.0.0.xml");

            LogicalPhysicalAlias alias = scan.logicalPhysicalAliases().stream()
                    .filter(a -> "db/changelog/db.changelog-1.0.0.xml".equals(a.logical()))
                    .findFirst()
                    .orElseThrow();

            assertThat(alias.physical()).isEqualTo("discovery/included-with-logical.xml");

            assertThat(scan.resourcePaths()).contains(alias.logical(), alias.physical());

            assertThat(LiquibaseClasspathResources.readAllBytesOrNull(
                    Thread.currentThread().getContextClassLoader(), alias.physical()))
                    .isNotNull();
        }
    }
}
