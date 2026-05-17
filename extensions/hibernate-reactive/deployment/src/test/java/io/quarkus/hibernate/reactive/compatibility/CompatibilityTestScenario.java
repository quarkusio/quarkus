package io.quarkus.hibernate.reactive.compatibility;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Defines all compatibility test scenarios for Hibernate ORM and Hibernate Reactive interoperability.
 * <p>
 * Each scenario specifies a distinct combination of:
 * <ul>
 * <li>Blocking (JDBC/Agroal) datasource configuration</li>
 * <li>Reactive datasource configuration</li>
 * <li>Default vs. named datasources</li>
 * <li>Default vs. named persistence units</li>
 * </ul>
 * This enum makes the test coverage matrix explicit and eliminates the need for
 * denormalized test class names to convey scenario intent.
 */
public enum CompatibilityTestScenario {

    /**
     * Default datasource, both blocking (JDBC/Agroal) and reactive enabled.
     * Uses the default persistence unit.
     */
    DEFAULT_BOTH {
        @Override
        public QuarkusExtensionTest configure(QuarkusExtensionTest test) {
            return test
                    .setForcedDependencies(java.util.List.of(JDBC_PG_DEPLOYMENT))
                    .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.datasource.reactive", "true")
                    .overrideConfigKey("quarkus.datasource.db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.password", USERNAME_PWD)
                    .overrideConfigKey("quarkus.hibernate-orm.log.format-sql", "false")
                    .overrideConfigKey("quarkus.hibernate-orm.log.highlight-sql", "false")
                    .overrideConfigKey("quarkus.log.category.\"org.hibernate.SQL\".level", "DEBUG");
        }
    },

    /**
     * Default datasource, blocking only (reactive explicitly disabled).
     * Uses the default persistence unit.
     */
    DEFAULT_BLOCKING_ONLY {
        @Override
        public QuarkusExtensionTest configure(QuarkusExtensionTest test) {
            return test
                    .setForcedDependencies(java.util.List.of(JDBC_PG_DEPLOYMENT))
                    .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.datasource.reactive", "false")
                    .overrideConfigKey("quarkus.datasource.db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.password", USERNAME_PWD);
        }
    },

    /**
     * Default datasource, reactive only. No JDBC driver — blocking is unavailable.
     * Uses the default persistence unit.
     */
    DEFAULT_REACTIVE_ONLY_NO_JDBC_DRIVER {
        @Override
        public QuarkusExtensionTest configure(QuarkusExtensionTest test) {
            // No forced JDBC driver dependency — disabling blocking via absence of driver
            return test
                    .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.datasource.reactive", "true")
                    .overrideConfigKey("quarkus.datasource.db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.password", USERNAME_PWD);
        }
    },

    /**
     * Default datasource, reactive only. JDBC driver present but blocking session factory
     * explicitly disabled via {@code quarkus.hibernate-orm.blocking=false}.
     * Uses the default persistence unit.
     */
    DEFAULT_REACTIVE_BLOCKING_SESSION_DISABLED {
        @Override
        public QuarkusExtensionTest configure(QuarkusExtensionTest test) {
            return test
                    .setForcedDependencies(java.util.List.of(JDBC_PG_DEPLOYMENT))
                    .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.hibernate-orm.blocking", "false")
                    .overrideConfigKey("quarkus.datasource.reactive", "true")
                    .overrideConfigKey("quarkus.datasource.db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.password", USERNAME_PWD);
        }
    },

    /**
     * Default datasource, reactive only. JDBC driver present but JDBC explicitly disabled
     * via {@code quarkus.datasource.jdbc=false}.
     * Uses the default persistence unit.
     */
    DEFAULT_REACTIVE_JDBC_DISABLED {
        @Override
        public QuarkusExtensionTest configure(QuarkusExtensionTest test) {
            return test
                    .setForcedDependencies(java.util.List.of(JDBC_PG_DEPLOYMENT))
                    .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.datasource.jdbc", "false")
                    .overrideConfigKey("quarkus.datasource.reactive", "true")
                    .overrideConfigKey("quarkus.datasource.db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.password", USERNAME_PWD);
        }
    },

    /**
     * Named datasource ("named-datasource"), both blocking (JDBC/Agroal) and reactive enabled.
     * Default persistence unit pointing to the named datasource.
     */
    NAMED_DATASOURCE_BOTH {
        @Override
        public QuarkusExtensionTest configure(QuarkusExtensionTest test) {
            return test
                    .setForcedDependencies(java.util.List.of(JDBC_PG_DEPLOYMENT))
                    .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.hibernate-orm.datasource", "named-datasource")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".reactive", "true")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".password", USERNAME_PWD);
        }
    },

    /**
     * Named datasource ("named-datasource") with a named persistence unit ("named-pu").
     * Both blocking and reactive enabled through the same named datasource.
     */
    NAMED_DATASOURCE_NAMED_PU_BOTH {
        @Override
        public QuarkusExtensionTest configure(QuarkusExtensionTest test) {
            return test
                    .setForcedDependencies(java.util.List.of(JDBC_PG_DEPLOYMENT))
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".schema-management.strategy",
                            SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".datasource", "named-datasource")
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".packages",
                            "io.quarkus.hibernate.reactive.entities")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".reactive", "true")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".password", USERNAME_PWD)
                    .overrideConfigKey("quarkus.log.category.\"io.quarkus.hibernate\".level", "DEBUG");
        }
    },

    /**
     * Named datasource ("named-datasource"), reactive only. No JDBC driver dependency.
     * Default persistence unit pointing to the named datasource.
     */
    NAMED_DATASOURCE_REACTIVE_ONLY {
        @Override
        public QuarkusExtensionTest configure(QuarkusExtensionTest test) {
            // No forced JDBC driver dependency
            return test
                    .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.hibernate-orm.datasource", "named-datasource")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".reactive", "true")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".password", USERNAME_PWD);
        }
    },

    /**
     * Named datasource ("named-datasource") provides reactive-only access for a named persistence unit
     * ("named-pu"), while the default datasource provides blocking-only access for the default PU.
     * Named datasource has {@code jdbc=false, reactive=true}; default datasource has {@code reactive=false}.
     */
    NAMED_REACTIVE_DEFAULT_BLOCKING {
        @Override
        public QuarkusExtensionTest configure(QuarkusExtensionTest test) {
            return test
                    .setForcedDependencies(java.util.List.of(JDBC_PG_DEPLOYMENT))
                    // Named datasource: reactive only
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".jdbc", "false")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".reactive", "true")
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".schema-management.strategy",
                            SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".datasource", "named-datasource")
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu\".packages",
                            "io.quarkus.hibernate.reactive.entities")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource\".password", USERNAME_PWD)
                    // Default datasource: blocking only (reactive=false, JDBC enabled by default)
                    .overrideConfigKey("quarkus.datasource.reactive", "false")
                    // Default PU: explicit packages needed when coexisting with named PU
                    .overrideConfigKey("quarkus.hibernate-orm.packages", "io.quarkus.hibernate.reactive.entities")
                    .overrideConfigKey("quarkus.hibernate-orm.database.generation", SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.datasource.username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.password", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.log.category.\"io.quarkus.hibernate\".level", "DEBUG");
        }
    },

    /**
     * Two entirely separate named datasources: "named-datasource-reactive" (reactive-only, jdbc=false)
     * backing "named-pu-reactive", and "named-datasource-blocking" (JDBC-only, reactive=false)
     * backing "named-pu-blocking".
     */
    DIFFERENT_NAMED_DATASOURCES_NAMED_PU_BOTH {
        @Override
        public QuarkusExtensionTest configure(QuarkusExtensionTest test) {
            return test
                    .setForcedDependencies(java.util.List.of(JDBC_PG_DEPLOYMENT))
                    // Reactive named datasource
                    .overrideConfigKey("quarkus.datasource.\"named-datasource-reactive\".jdbc", "false")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource-reactive\".reactive", "true")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource-reactive\".db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource-reactive\".username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource-reactive\".password", USERNAME_PWD)
                    // Reactive named persistence unit
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-reactive\".schema-management.strategy",
                            SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-reactive\".datasource",
                            "named-datasource-reactive")
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-reactive\".packages",
                            "io.quarkus.hibernate.reactive.entities")
                    // Blocking named datasource
                    .overrideConfigKey("quarkus.datasource.\"named-datasource-blocking\".jdbc", "true")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource-blocking\".reactive", "false")
                    .overrideConfigKey("quarkus.datasource.\"named-datasource-blocking\".db-kind", POSTGRES_KIND)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource-blocking\".username", USERNAME_PWD)
                    .overrideConfigKey("quarkus.datasource.\"named-datasource-blocking\".password", USERNAME_PWD)
                    // Blocking named persistence unit
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-blocking\".schema-management.strategy",
                            SCHEMA_MANAGEMENT_STRATEGY)
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-blocking\".datasource",
                            "named-datasource-blocking")
                    .overrideConfigKey("quarkus.hibernate-orm.\"named-pu-blocking\".packages",
                            "io.quarkus.hibernate.reactive.entities")
                    .overrideConfigKey("quarkus.log.category.\"io.quarkus.hibernate\".level", "DEBUG");
        }
    };

    // -------------------------------------------------------------------------
    // Shared constants (mirrors CompatibilityUnitTestBase)
    // -------------------------------------------------------------------------

    public static final String POSTGRES_KIND = CompatibilityUnitTestBase.POSTGRES_KIND;
    public static final String USERNAME_PWD = CompatibilityUnitTestBase.USERNAME_PWD;
    public static final String SCHEMA_MANAGEMENT_STRATEGY = CompatibilityUnitTestBase.SCHEMA_MANAGEMENT_STRATEGY;

    static final Dependency JDBC_PG_DEPLOYMENT = Dependency.of(
            "io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion());

    // -------------------------------------------------------------------------
    // Abstract method
    // -------------------------------------------------------------------------

    /**
     * Applies this scenario's Quarkus configuration to the given test extension.
     *
     * @param test the base {@link QuarkusExtensionTest} to configure (created via {@link #baseTest()})
     * @return the configured test extension
     */
    public abstract QuarkusExtensionTest configure(QuarkusExtensionTest test);

    // -------------------------------------------------------------------------
    // Factory: common base test setup
    // -------------------------------------------------------------------------

    /**
     * Creates the base {@link QuarkusExtensionTest} with the standard application root
     * (Hero entity + import.sql). Pass the result to {@link #configure(QuarkusExtensionTest)}
     * to produce a fully configured test extension.
     */
    public static QuarkusExtensionTest baseTest() {
        return new QuarkusExtensionTest()
                .withApplicationRoot(jar -> jar
                        .addClasses(Hero.class)
                        .addAsResource("complexMultilineImports.sql", "import.sql"));
    }
}
