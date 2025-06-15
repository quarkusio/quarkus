package io.quarkus.hibernate.orm.mapping.id.optimizer;

import org.hibernate.id.enhanced.PooledLoOptimizer;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.test.QuarkusUnitTest;

public class IdOptimizerDefaultPooledLoTest extends AbstractIdOptimizerDefaultTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(EntityWithDefaultGenerator.class,
                    EntityWithGenericGenerator.class, EntityWithSequenceGenerator.class, EntityWithTableGenerator.class,
                    EntityWithGenericGeneratorAndPooledOptimizer.class,
                    EntityWithGenericGeneratorAndPooledLoOptimizer.class).addClasses(SchemaUtil.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.id.optimizer.default", "pooled-lo");

    @Override
    Class<?> defaultOptimizerType() {
        return PooledLoOptimizer.class;
    }
}
