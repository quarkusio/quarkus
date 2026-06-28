package io.quarkus.hibernate.reactive.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.LogRecord;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.arc.Arc;
import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.vertx.sqlclient.Pool;

/**
 * Tests ORM/Reactive compatibility for all 10 supported configuration scenarios.
 *
 * <p>
 * Scenario matrix:
 *
 * <pre>
 * | Scenario name                              | Reactive | Blocking | Notes                             |
 * |--------------------------------------------|----------|----------|-----------------------------------|
 * | DEFAULT_BOTH                               |    yes   |    yes   | verifies DDL runs only once       |
 * | DEFAULT_BLOCKING_ONLY                      |    no    |    yes   | reactive explicitly disabled      |
 * | DEFAULT_REACTIVE_ONLY_NO_JDBC_DRIVER       |    yes   |    no    | no JDBC driver on classpath       |
 * | DEFAULT_REACTIVE_BLOCKING_SESSION_DISABLED |    yes   |    no    | blocking=false config             |
 * | DEFAULT_REACTIVE_JDBC_DISABLED             |    yes   |    no    | jdbc=false config                 |
 * | NAMED_DATASOURCE_BOTH                      |    yes   |    yes   | named datasource, default PU      |
 * | NAMED_DATASOURCE_NAMED_PU_BOTH             |    yes   |    yes   | named datasource and named PU     |
 * | NAMED_DATASOURCE_REACTIVE_ONLY             |    yes   |    no    | verifies named reactive pool      |
 * | NAMED_REACTIVE_DEFAULT_BLOCKING            |    yes   |    yes   | named reactive PU + default blocking |
 * | DIFFERENT_NAMED_DATASOURCES_NAMED_PU_BOTH  |    yes   |    yes   | 2 named datasources, 2 named PUs  |
 * </pre>
 */
@ParameterizedClass
@MethodSource("scenarios")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ORMReactiveCompatibilityTest extends CompatibilityUnitTestBase {

    // Purely nominal: configuration lives inline in @BeforeParameterizedClassInvocation below.
    enum Scenario {
        DEFAULT_BOTH,
        DEFAULT_BLOCKING_ONLY,
        DEFAULT_REACTIVE_ONLY_NO_JDBC_DRIVER,
        DEFAULT_REACTIVE_BLOCKING_SESSION_DISABLED,
        DEFAULT_REACTIVE_JDBC_DISABLED,
        NAMED_DATASOURCE_BOTH,
        NAMED_DATASOURCE_NAMED_PU_BOTH,
        NAMED_DATASOURCE_REACTIVE_ONLY,
        NAMED_REACTIVE_DEFAULT_BLOCKING,
        DIFFERENT_NAMED_DATASOURCES_NAMED_PU_BOTH
    }

    private static final List<Dependency> JDBC_DEPS = List.of(
            Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion()));

    // Common connection properties shared across all default-datasource scenarios.
    private static final String DEFAULT_DS_CREDENTIALS = """
            quarkus.datasource.db-kind=postgresql
            quarkus.datasource.username=hibernate_orm_test
            quarkus.datasource.password=hibernate_orm_test
            """;

    // Common connection properties shared across named-datasource scenarios.
    private static final String NAMED_DS_CREDENTIALS = """
            quarkus.datasource."named-datasource".db-kind=postgresql
            quarkus.datasource."named-datasource".username=hibernate_orm_test
            quarkus.datasource."named-datasource".password=hibernate_orm_test
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"));

    @Parameter(0)
    public static Scenario scenario;

    // Assigned in @BeforeParameterizedClassInvocation (before Quarkus starts) as lambdas that capture
    // the CDI lookup to perform. Executed in @BeforeEach after the CDI container is available.
    // A null supplier indicates that capability is disabled for this scenario.
    private static Supplier<Mutiny.SessionFactory> reactiveSfSupplier;
    private static Supplier<SessionFactory> blockingSfSupplier;
    private static Supplier<Pool> namedPoolSupplier;

    // Resolved per test method from the suppliers above.
    private Mutiny.SessionFactory reactiveSf;
    private SessionFactory blockingSf;
    private Pool namedPool;

    public static List<Arguments> scenarios() {
        return Arrays.stream(Scenario.values()).map(Arguments::of).toList();
    }

    @BeforeParameterizedClassInvocation
    static void configure() {
        config.resetForParameterizedClass();
        reactiveSfSupplier = null;
        blockingSfSupplier = null;
        namedPoolSupplier = null;

        switch (scenario) {
            case DEFAULT_BOTH -> {
                config.setForcedDependencies(JDBC_DEPS)
                        .withConfiguration(DEFAULT_DS_CREDENTIALS + """
                                quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                                quarkus.datasource.reactive=true
                                quarkus.hibernate-orm.log.format-sql=false
                                quarkus.hibernate-orm.log.highlight-sql=false
                                quarkus.log.category."org.hibernate.SQL".level=DEBUG
                                """)
                        .setLogRecordPredicate(record -> "org.hibernate.SQL".equals(record.getLoggerName()))
                        .assertLogRecords(records -> assertThat(records.stream().map(LogRecord::getMessage))
                                .containsOnlyOnce("create sequence hero_SEQ start with 1 increment by 50"));
                reactiveSfSupplier = () -> Arc.container().instance(Mutiny.SessionFactory.class).get();
                blockingSfSupplier = () -> Arc.container().instance(SessionFactory.class).get();
            }
            case DEFAULT_BLOCKING_ONLY -> {
                config.setForcedDependencies(JDBC_DEPS)
                        .withConfiguration(DEFAULT_DS_CREDENTIALS + """
                                quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                                quarkus.datasource.reactive=false
                                """);
                blockingSfSupplier = () -> Arc.container().instance(SessionFactory.class).get();
            }
            case DEFAULT_REACTIVE_ONLY_NO_JDBC_DRIVER -> {
                config.setForcedDependencies(List.of())
                        .withConfiguration(DEFAULT_DS_CREDENTIALS + """
                                quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                                quarkus.datasource.reactive=true
                                """);
                reactiveSfSupplier = () -> Arc.container().instance(Mutiny.SessionFactory.class).get();
            }
            case DEFAULT_REACTIVE_BLOCKING_SESSION_DISABLED -> {
                config.setForcedDependencies(JDBC_DEPS)
                        .withConfiguration(DEFAULT_DS_CREDENTIALS + """
                                quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                                quarkus.hibernate-orm.blocking=false
                                quarkus.datasource.reactive=true
                                """);
                reactiveSfSupplier = () -> Arc.container().instance(Mutiny.SessionFactory.class).get();
            }
            case DEFAULT_REACTIVE_JDBC_DISABLED -> {
                config.setForcedDependencies(JDBC_DEPS)
                        .withConfiguration(DEFAULT_DS_CREDENTIALS + """
                                quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                                quarkus.datasource.jdbc=false
                                quarkus.datasource.reactive=true
                                """);
                reactiveSfSupplier = () -> Arc.container().instance(Mutiny.SessionFactory.class).get();
            }
            case NAMED_DATASOURCE_BOTH -> {
                config.setForcedDependencies(JDBC_DEPS)
                        .withConfiguration(NAMED_DS_CREDENTIALS + """
                                quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                                quarkus.hibernate-orm.datasource=named-datasource
                                quarkus.datasource."named-datasource".reactive=true
                                """);
                reactiveSfSupplier = () -> Arc.container().instance(Mutiny.SessionFactory.class).get();
                blockingSfSupplier = () -> Arc.container().instance(SessionFactory.class).get();
            }
            case NAMED_DATASOURCE_NAMED_PU_BOTH -> {
                config.setForcedDependencies(JDBC_DEPS)
                        .withConfiguration(NAMED_DS_CREDENTIALS + """
                                quarkus.hibernate-orm."named-pu".schema-management.strategy=drop-and-create
                                quarkus.hibernate-orm."named-pu".datasource=named-datasource
                                quarkus.hibernate-orm."named-pu".packages=io.quarkus.hibernate.reactive.entities
                                quarkus.datasource."named-datasource".reactive=true
                                quarkus.log.category."io.quarkus.hibernate".level=DEBUG
                                """);
                reactiveSfSupplier = () -> Arc.container()
                        .select(Mutiny.SessionFactory.class, new PersistenceUnit.PersistenceUnitLiteral("named-pu"))
                        .get();
                blockingSfSupplier = () -> Arc.container()
                        .select(SessionFactory.class, new PersistenceUnit.PersistenceUnitLiteral("named-pu"))
                        .get();
            }
            case NAMED_DATASOURCE_REACTIVE_ONLY -> {
                config.setForcedDependencies(List.of())
                        .withConfiguration(NAMED_DS_CREDENTIALS + """
                                quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                                quarkus.hibernate-orm.datasource=named-datasource
                                quarkus.datasource."named-datasource".reactive=true
                                """);
                reactiveSfSupplier = () -> Arc.container().instance(Mutiny.SessionFactory.class).get();
                namedPoolSupplier = () -> Arc.container()
                        .select(Pool.class, new ReactiveDataSource.ReactiveDataSourceLiteral("named-datasource"))
                        .get();
            }
            case NAMED_REACTIVE_DEFAULT_BLOCKING -> {
                config.setForcedDependencies(JDBC_DEPS)
                        .withConfiguration(NAMED_DS_CREDENTIALS + DEFAULT_DS_CREDENTIALS + """
                                quarkus.datasource."named-datasource".jdbc=false
                                quarkus.datasource."named-datasource".reactive=true
                                quarkus.hibernate-orm."named-pu".schema-management.strategy=drop-and-create
                                quarkus.hibernate-orm."named-pu".datasource=named-datasource
                                quarkus.hibernate-orm."named-pu".packages=io.quarkus.hibernate.reactive.entities
                                quarkus.datasource.reactive=false
                                quarkus.hibernate-orm.packages=io.quarkus.hibernate.reactive.entities
                                quarkus.hibernate-orm.database.generation=drop-and-create
                                quarkus.log.category."io.quarkus.hibernate".level=DEBUG
                                """);
                reactiveSfSupplier = () -> Arc.container()
                        .select(Mutiny.SessionFactory.class, new PersistenceUnit.PersistenceUnitLiteral("named-pu"))
                        .get();
                blockingSfSupplier = () -> Arc.container().instance(SessionFactory.class).get();
            }
            case DIFFERENT_NAMED_DATASOURCES_NAMED_PU_BOTH -> {
                config.setForcedDependencies(JDBC_DEPS)
                        .withConfiguration("""
                                quarkus.datasource."named-datasource-reactive".jdbc=false
                                quarkus.datasource."named-datasource-reactive".reactive=true
                                quarkus.datasource."named-datasource-reactive".db-kind=postgresql
                                quarkus.datasource."named-datasource-reactive".username=hibernate_orm_test
                                quarkus.datasource."named-datasource-reactive".password=hibernate_orm_test
                                quarkus.hibernate-orm."named-pu-reactive".schema-management.strategy=drop-and-create
                                quarkus.hibernate-orm."named-pu-reactive".datasource=named-datasource-reactive
                                quarkus.hibernate-orm."named-pu-reactive".packages=io.quarkus.hibernate.reactive.entities
                                quarkus.datasource."named-datasource-blocking".jdbc=true
                                quarkus.datasource."named-datasource-blocking".reactive=false
                                quarkus.datasource."named-datasource-blocking".db-kind=postgresql
                                quarkus.datasource."named-datasource-blocking".username=hibernate_orm_test
                                quarkus.datasource."named-datasource-blocking".password=hibernate_orm_test
                                quarkus.hibernate-orm."named-pu-blocking".schema-management.strategy=drop-and-create
                                quarkus.hibernate-orm."named-pu-blocking".datasource=named-datasource-blocking
                                quarkus.hibernate-orm."named-pu-blocking".packages=io.quarkus.hibernate.reactive.entities
                                quarkus.log.category."io.quarkus.hibernate".level=DEBUG
                                """);
                reactiveSfSupplier = () -> Arc.container()
                        .select(Mutiny.SessionFactory.class,
                                new PersistenceUnit.PersistenceUnitLiteral("named-pu-reactive"))
                        .get();
                blockingSfSupplier = () -> Arc.container()
                        .select(SessionFactory.class,
                                new PersistenceUnit.PersistenceUnitLiteral("named-pu-blocking"))
                        .get();
            }
        }
    }

    @BeforeEach
    void resolveFromContainer() {
        reactiveSf = reactiveSfSupplier != null ? reactiveSfSupplier.get() : null;
        blockingSf = blockingSfSupplier != null ? blockingSfSupplier.get() : null;
        namedPool = namedPoolSupplier != null ? namedPoolSupplier.get() : null;
    }

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter asserter) {
        if (reactiveSf != null) {
            testReactiveWorks(reactiveSf, asserter);
            if (namedPool != null) {
                assertThat(namedPool).isNotNull();
            }
        } else {
            testReactiveDisabled();
        }
    }

    @Test
    public void testBlocking() {
        if (blockingSf != null) {
            testBlockingWorks(blockingSf);
        } else {
            testBlockingDisabled();
        }
    }
}
