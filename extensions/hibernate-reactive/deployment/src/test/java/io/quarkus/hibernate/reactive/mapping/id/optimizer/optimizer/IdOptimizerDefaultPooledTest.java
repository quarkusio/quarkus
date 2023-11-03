package io.quarkus.hibernate.reactive.mapping.id.optimizer.optimizer;

import org.hibernate.id.enhanced.PooledOptimizer;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.SchemaUtil;
import io.quarkus.test.QuarkusUnitTest;

public class IdOptimizerDefaultPooledTest extends AbstractIdOptimizerDefaultTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EntityWithDefaultGenerator.class, EntityWithGenericGenerator.class,
                            EntityWithSequenceGenerator.class, EntityWithTableGenerator.class,
                            EntityWithGenericGeneratorAndPooledOptimizer.class,
                            EntityWithGenericGeneratorAndPooledLoOptimizer.class)
                    .addClasses(SchemaUtil.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.id.optimizer.default", "pooled");

    @Override
    Class<?> defaultOptimizerType() {
        return PooledOptimizer.class;
    }
}
