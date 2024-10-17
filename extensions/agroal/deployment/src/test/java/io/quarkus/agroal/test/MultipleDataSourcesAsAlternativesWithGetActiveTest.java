package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests a use case where multiple datasources are defined at build time,
 * but only one is used at runtime,
 * and Arc's {@code getActive()} method is used to retrieve the active datasource.
 * <p>
 * This is mostly useful when each datasource has a distinct db-kind, but in theory that shouldn't matter,
 * so we use the h2 db-kind everywhere here to keep test dependencies simpler.
 */
public abstract class MultipleDataSourcesAsAlternativesWithGetActiveTest {

    public static class Ds1ActiveTest extends MultipleDataSourcesAsAlternativesWithBeanProducerTest {
        @RegisterExtension
        static QuarkusUnitTest runner = runner("ds-1");

        public Ds1ActiveTest() {
            super("ds-1", "ds-2");
        }
    }

    public static class Ds2ActiveTest extends MultipleDataSourcesAsAlternativesWithBeanProducerTest {
        @RegisterExtension
        static QuarkusUnitTest runner = runner("ds-2");

        public Ds2ActiveTest() {
            super("ds-2", "ds-1");
        }
    }

    static QuarkusUnitTest runner(String activeDsName) {
        return new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addClass(MyProducer.class))
                .overrideConfigKey("quarkus.datasource.ds-1.db-kind", "h2")
                .overrideConfigKey("quarkus.datasource.ds-1.active", "false")
                .overrideConfigKey("quarkus.datasource.ds-2.db-kind", "h2")
                .overrideConfigKey("quarkus.datasource.ds-2.active", "false")
                // This is where we select the active datasource
                .overrideRuntimeConfigKey("quarkus.datasource." + activeDsName + ".active", "true")
                .overrideRuntimeConfigKey("quarkus.datasource." + activeDsName + ".jdbc.url", "jdbc:h2:mem:testds1");
    }

    private final String activeDsName;
    private final String inactiveDsName;

    protected MultipleDataSourcesAsAlternativesWithGetActiveTest(String activeDsName, String inactiveDsName) {
        this.activeDsName = activeDsName;
        this.inactiveDsName = inactiveDsName;
    }

    @Inject
    @Any
    InjectableInstance<AgroalDataSource> datasourceInjectableInstance;

    @Test
    public void testExplicitDatasourceBeanUsable() {
        doTestDatasource(Arc.container()
                .select(AgroalDataSource.class, new DataSource.DataSourceLiteral(activeDsName)).get());
    }

    @Test
    public void testInjectableInstanceGetActiveBeanUsable() {
        doTestDatasource(datasourceInjectableInstance.getActive());
    }

    @Test
    public void testInactiveDatasourceBeanUnusable() {
        assertThatThrownBy(
                () -> Arc.container().select(AgroalDataSource.class, new DataSource.DataSourceLiteral(inactiveDsName)).get()
                        .getConnection())
                .hasMessageContaining(
                        "Datasource '" + inactiveDsName + "' was deactivated through configuration properties.");
    }

    private static void doTestDatasource(AgroalDataSource dataSource) {
        assertThatCode(() -> {
            try (var connection = dataSource.getConnection()) {
            }
        })
                .doesNotThrowAnyException();
    }

    private static class MyProducer {
        @Inject
        @DataSource("ds-1")
        InjectableInstance<AgroalDataSource> dataSource1Bean;

        @Inject
        @DataSource("ds-2")
        InjectableInstance<AgroalDataSource> dataSource2Bean;

        @Produces
        @ApplicationScoped
        public AgroalDataSource dataSource() {
            if (dataSource1Bean.getHandle().getBean().isActive()) {
                return dataSource1Bean.get();
            } else if (dataSource2Bean.getHandle().getBean().isActive()) {
                return dataSource2Bean.get();
            } else {
                throw new RuntimeException("No active datasource!");
            }
        }
    }
}
