package io.quarkus.hibernate.reactive.mapping.id.optimizer.optimizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.PooledLoOptimizer;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.reactive.id.impl.ReactiveGeneratorWrapper;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.reactive.SchemaUtil;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

public abstract class AbstractIdOptimizerDefaultTest {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    abstract Class<?> defaultOptimizerType();

    @Test
    public void defaults() {
        assertThat(List.of(
                EntityWithDefaultGenerator.class,
                EntityWithGenericGenerator.class,
                EntityWithSequenceGenerator.class,
                EntityWithTableGenerator.class))
                .allSatisfy(c -> assertOptimizer(c).isInstanceOf(defaultOptimizerType()));
    }

    @Test
    public void explicitOverrides() {
        assertOptimizer(EntityWithGenericGeneratorAndPooledOptimizer.class)
                .isInstanceOf(PooledOptimizer.class);
        assertOptimizer(EntityWithGenericGeneratorAndPooledLoOptimizer.class)
                .isInstanceOf(PooledLoOptimizer.class);
    }

    @Test
    @RunOnVertxContext
    public void ids(UniAsserter asserter) {
        for (long i = 1; i <= 51; i++) {
            long expectedId = i;
            // Apparently, we can rely on assertions being executed in order.
            asserter.assertThat(() -> sessionFactory.withTransaction(s -> {
                var entity = new EntityWithSequenceGenerator();
                return s.persist(entity).replaceWith(() -> entity.id);
            }),
                    id -> assertThat(id).isEqualTo(expectedId));
        }
    }

    AbstractObjectAssert<?, Optimizer> assertOptimizer(Class<?> entityType) {
        return assertThat(SchemaUtil.getGenerator(entityType, SchemaUtil.mappingMetamodel(sessionFactory)))
                .as("Reactive ID generator wrapper for entity type " + entityType.getSimpleName())
                .asInstanceOf(InstanceOfAssertFactories.type(ReactiveGeneratorWrapper.class))
                .extracting("generator") // Needs reflection, unfortunately the blocking generator is not exposed...
                .as("Blocking ID generator for entity type " + entityType.getSimpleName())
                .asInstanceOf(InstanceOfAssertFactories.type(OptimizableGenerator.class))
                .extracting(OptimizableGenerator::getOptimizer)
                .as("ID optimizer for entity type " + entityType.getSimpleName());
    }
}
