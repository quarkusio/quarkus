package io.quarkus.hibernate.orm.mapping.id.optimizer;

import org.hibernate.id.enhanced.NoopOptimizer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.test.QuarkusUnitTest;

public class IdOptimizerDefaultNoneTest extends AbstractIdOptimizerDefaultTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(EntityWithDefaultGenerator.class,
                    EntityWithGenericGenerator.class, EntityWithSequenceGenerator.class, EntityWithTableGenerator.class,
                    EntityWithGenericGeneratorAndPooledOptimizer.class,
                    EntityWithGenericGeneratorAndPooledLoOptimizer.class).addClasses(SchemaUtil.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.id.optimizer.default", "none");

    @Override
    @Disabled("The 'none' optimizer will produce a different stream of IDs (1 then 51 then 101 then ...)")
    public void ids() {
        super.ids();
    }

    @Override
    Class<?> defaultOptimizerType() {
        return NoopOptimizer.class;
    }
}
