package io.quarkus.hibernate.orm.deployment;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Set;

import org.hibernate.dialect.DB297Dialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect;

public class ConstantsTest {
    @ParameterizedTest
    @MethodSource("provideConstantsToTest")
    void testClassNameRefersToExistingClass(DotName constant) {
        assertThatCode(() -> getClass().getClassLoader().loadClass(constant.toString()))
                .doesNotThrowAnyException();
    }

    private static Set<DotName> provideConstantsToTest() {
        return ClassNames.CREATED_CONSTANTS;
    }

    @Test
    public void testDialectNames() {
        assertDialectMatch(DatabaseKind.DB2, DB297Dialect.class);
        assertDialectMatch(DatabaseKind.POSTGRESQL, QuarkusPostgreSQL10Dialect.class);
        assertDialectMatch(DatabaseKind.H2, QuarkusH2Dialect.class);
        assertDialectMatch(DatabaseKind.MARIADB, MariaDB103Dialect.class);
        assertDialectMatch(DatabaseKind.MYSQL, MySQL8Dialect.class);
        assertDialectMatch(DatabaseKind.DERBY, DerbyTenSevenDialect.class);
        assertDialectMatch(DatabaseKind.MSSQL, SQLServer2012Dialect.class);
        assertDialectMatch(DatabaseKind.ORACLE, Oracle12cDialect.class);
    }

    private void assertDialectMatch(String dbName, Class<?> dialectClass) {
        final String guessDialect = Dialects.guessDialect(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, dbName);
        Assertions.assertEquals(dialectClass.getName(), guessDialect);
    }

}
