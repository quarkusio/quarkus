package io.quarkus.hibernate.reactive.mapping.id.optimizer.optimizer;

import org.hibernate.id.enhanced.NoopOptimizer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.SchemaUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.UniAsserter;

public class IdOptimizerDefaultNoneTest extends AbstractIdOptimizerDefaultTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EntityWithDefaultGenerator.class, EntityWithGenericGenerator.class,
                            EntityWithSequenceGenerator.class, EntityWithTableGenerator.class,
                            EntityWithGenericGeneratorAndPooledOptimizer.class,
                            EntityWithGenericGeneratorAndPooledLoOptimizer.class)
                    .addClasses(SchemaUtil.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.id.optimizer.default", "none");

    @Override
    @Test
    @Disabled("The 'none' optimizer will produce a different stream of IDs (1 then 51 then 101 then ...)")
    public void ids(UniAsserter asserter) {
        super.ids(asserter);
    }

    @Override
    Class<?> defaultOptimizerType() {
        return NoopOptimizer.class;
    }
}
