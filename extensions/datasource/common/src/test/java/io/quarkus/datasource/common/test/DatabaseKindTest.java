package io.quarkus.datasource.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.common.runtime.DatabaseKind;

public class DatabaseKindTest {

    @Test
    public void testNormalize() {
        assertEquals(DatabaseKind.DB2, DatabaseKind.normalize("db2"));
        assertEquals(DatabaseKind.DERBY, DatabaseKind.normalize("derby"));
        assertEquals(DatabaseKind.H2, DatabaseKind.normalize("h2"));
        assertEquals(DatabaseKind.MARIADB, DatabaseKind.normalize("mariadb"));
        assertEquals(DatabaseKind.MSSQL, DatabaseKind.normalize("mssql"));
        assertEquals(DatabaseKind.MYSQL, DatabaseKind.normalize("mysql"));
        assertEquals(DatabaseKind.POSTGRESQL, DatabaseKind.normalize("postgresql"));
        assertEquals(DatabaseKind.POSTGRESQL, DatabaseKind.normalize("Postgresql"));
        assertEquals(DatabaseKind.POSTGRESQL, DatabaseKind.normalize("pg"));
        assertEquals(DatabaseKind.POSTGRESQL, DatabaseKind.normalize("pgsql"));

        assertNull(DatabaseKind.normalize("   "));
    }

    @Test
    public void testIs() {
        assertTrue(DatabaseKind.isDB2("db2"));
        assertFalse(DatabaseKind.isDB2("mariadb"));

        assertTrue(DatabaseKind.isDerby("derby"));
        assertFalse(DatabaseKind.isDerby("mariadb"));

        assertTrue(DatabaseKind.isH2("h2"));
        assertFalse(DatabaseKind.isH2("mariadb"));

        assertTrue(DatabaseKind.isMariaDB("mariadb"));
        assertFalse(DatabaseKind.isMariaDB("mysql"));
        assertFalse(DatabaseKind.isMariaDB(DatabaseKind.POSTGRESQL));

        assertTrue(DatabaseKind.isMsSQL(DatabaseKind.MSSQL));
        assertFalse(DatabaseKind.isMsSQL("mysql"));

        assertTrue(DatabaseKind.isMySQL("mysql"));
        assertFalse(DatabaseKind.isMySQL("mariadb"));
        assertFalse(DatabaseKind.isMySQL(DatabaseKind.POSTGRESQL));

        assertTrue(DatabaseKind.isOracle("oracle"));
        assertFalse(DatabaseKind.isOracle("mariadb"));
        assertFalse(DatabaseKind.isOracle(DatabaseKind.POSTGRESQL));

        assertTrue(DatabaseKind.isPostgreSQL(DatabaseKind.POSTGRESQL));
        assertFalse(DatabaseKind.isPostgreSQL("mysql"));
    }
}
