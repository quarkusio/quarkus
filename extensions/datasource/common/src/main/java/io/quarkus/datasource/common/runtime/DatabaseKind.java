package io.quarkus.datasource.common.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * We don't use an enum as we also need to support non built-in database kinds.
 */
public final class DatabaseKind {

    public static final String DB2 = "db2";
    public static final String DERBY = "derby";
    public static final String H2 = "h2";
    public static final String MARIADB = "mariadb";
    public static final String MSSQL = "mssql";
    public static final String MYSQL = "mysql";
    public static final String POSTGRESQL = "postgresql";
    public static final String ORACLE = "oracle";

    private static final Map<String, String> ALIASES;

    static {
        Map<String, String> aliases = new HashMap<>();
        for (SupportedDatabaseKind kind : SupportedDatabaseKind.values()) {
            for (String alias : kind.aliases) {
                aliases.put(alias.toLowerCase(Locale.ROOT), kind.mainName.toLowerCase(Locale.ROOT));
            }
        }
        ALIASES = Collections.unmodifiableMap(aliases);
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String lowerCaseValue = value.toLowerCase(Locale.ROOT).trim();

        if (lowerCaseValue.isEmpty()) {
            return null;
        }

        String supportedValue = ALIASES.get(lowerCaseValue);
        if (supportedValue != null) {
            return supportedValue;
        }
        return lowerCaseValue;
    }

    public static boolean isDB2(String value) {
        return is(value, DB2);
    }

    public static boolean isDerby(String value) {
        return is(value, DERBY);
    }

    public static boolean isH2(String value) {
        return is(value, H2);
    }

    public static boolean isMariaDB(String value) {
        return is(value, MARIADB);
    }

    public static boolean isMsSQL(String value) {
        return is(value, MSSQL);
    }

    public static boolean isMySQL(String value) {
        return is(value, MYSQL);
    }

    public static boolean isOracle(String value) {
        return is(value, ORACLE);
    }

    public static boolean isPostgreSQL(String value) {
        return is(value, POSTGRESQL);
    }

    private static boolean is(String value, String mainName) {
        if (value == null) {
            return false;
        }

        String normalizedValue = normalize(value);

        return mainName.equals(normalizedValue);
    }

    private DatabaseKind() {
    }

    private enum SupportedDatabaseKind {
        DB2(DatabaseKind.DB2),
        DERBY(DatabaseKind.DERBY),
        H2(DatabaseKind.H2),
        MARIADB(DatabaseKind.MARIADB),
        MSSQL(DatabaseKind.MSSQL),
        MYSQL(DatabaseKind.MYSQL),
        ORACLE(DatabaseKind.ORACLE),
        POSTGRESQL(DatabaseKind.POSTGRESQL, "pgsql", "pg");

        private final String mainName;
        private final Set<String> aliases;

        private SupportedDatabaseKind(String mainName) {
            this.mainName = mainName;
            this.aliases = Collections.singleton(mainName);
        }

        private SupportedDatabaseKind(String mainName, String... aliases) {
            this.mainName = mainName;
            this.aliases = new HashSet<>();
            this.aliases.add(mainName);
            this.aliases.addAll(Arrays.asList(aliases));
        }
    }
}
