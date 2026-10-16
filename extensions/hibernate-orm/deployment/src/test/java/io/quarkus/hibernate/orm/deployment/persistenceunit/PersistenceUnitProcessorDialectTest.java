package io.quarkus.hibernate.orm.deployment.persistenceunit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;

class PersistenceUnitProcessorDialectTest {

    @Test
    void registersMetadataForEveryCoreDialect() {
        var dialects = new ArrayList<DatabaseKindDialectBuildItem>();

        new PersistenceUnitProcessor().registerHibernateOrmMetadataForCoreDialects(dialects::add);

        assertThat(dialects).extracting(DatabaseKindDialectBuildItem::getDbKind).containsExactlyInAnyOrder(
                DatabaseKind.DB2, DatabaseKind.H2, DatabaseKind.MARIADB, DatabaseKind.MSSQL, DatabaseKind.MYSQL,
                DatabaseKind.ORACLE, DatabaseKind.POSTGRESQL);
    }
}
