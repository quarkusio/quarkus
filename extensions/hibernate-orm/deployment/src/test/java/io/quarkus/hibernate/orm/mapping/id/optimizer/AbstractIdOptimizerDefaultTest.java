package io.quarkus.hibernate.orm.mapping.id.optimizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.PooledLoOptimizer;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.narayana.jta.QuarkusTransaction;

public abstract class AbstractIdOptimizerDefaultTest {

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    abstract Class<?> defaultOptimizerType();

    @Test
    public void defaults() {
        assertThat(List.of(EntityWithDefaultGenerator.class, EntityWithGenericGenerator.class,
                EntityWithSequenceGenerator.class, EntityWithTableGenerator.class))
                .allSatisfy(c -> assertOptimizer(c).isInstanceOf(defaultOptimizerType()));
    }

    @Test
    public void explicitOverrides() {
        assertOptimizer(EntityWithGenericGeneratorAndPooledOptimizer.class).isInstanceOf(PooledOptimizer.class);
        assertOptimizer(EntityWithGenericGeneratorAndPooledLoOptimizer.class).isInstanceOf(PooledLoOptimizer.class);
    }

    @Test
    public void ids() {
        for (long i = 1; i <= 51; i++) {
            assertThat(QuarkusTransaction.requiringNew().call(() -> {
                var entity = new EntityWithSequenceGenerator();
                session.persist(entity);
                return entity.id;
            })).isEqualTo(i);
        }
    }

    AbstractObjectAssert<?, Optimizer> assertOptimizer(Class<?> entityType) {
        return assertThat(SchemaUtil.getGenerator(sessionFactory, entityType))
                .as("ID generator for entity type " + entityType.getSimpleName())
                .asInstanceOf(InstanceOfAssertFactories.type(OptimizableGenerator.class))
                .extracting(OptimizableGenerator::getOptimizer)
                .as("ID optimizer for entity type " + entityType.getSimpleName());
    }
}
