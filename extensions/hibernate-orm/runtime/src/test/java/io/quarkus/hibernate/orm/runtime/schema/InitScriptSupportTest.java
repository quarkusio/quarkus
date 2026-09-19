package io.quarkus.hibernate.orm.runtime.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfigPersistenceUnit.DataManagementStrategy;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfigPersistenceUnit.HibernateGenerationStrategy;
import io.quarkus.runtime.LaunchMode;

/**
 * Outside of dev and test modes (the launch mode of a plain unit test is {@link LaunchMode#NORMAL}),
 * the data init script is only executed by default when Hibernate ORM creates the schema.
 */
class InitScriptSupportTest {

    @ParameterizedTest
    @EnumSource(value = HibernateGenerationStrategy.class, names = { "CREATE", "DROP_AND_CREATE" })
    void defaultStrategy_whenHibernateCreatesTheSchema(HibernateGenerationStrategy schemaManagementStrategy) {
        assertThat(LaunchMode.current()).isEqualTo(LaunchMode.NORMAL);
        assertThat(InitScriptSupport.defaultDataManagementStrategy(schemaManagementStrategy))
                .isEqualTo(DataManagementStrategy.CREATE);
    }

    @ParameterizedTest
    @EnumSource(value = HibernateGenerationStrategy.class, names = { "NONE", "UPDATE", "VALIDATE", "DROP" })
    void defaultStrategy_whenHibernateDoesNotCreateTheSchema(HibernateGenerationStrategy schemaManagementStrategy) {
        assertThat(LaunchMode.current()).isEqualTo(LaunchMode.NORMAL);
        assertThat(InitScriptSupport.defaultDataManagementStrategy(schemaManagementStrategy))
                .isEqualTo(DataManagementStrategy.NONE);
    }

    @Test
    void everySchemaManagementStrategyHasADefault() {
        for (HibernateGenerationStrategy strategy : HibernateGenerationStrategy.values()) {
            assertThat(InitScriptSupport.defaultDataManagementStrategy(strategy)).isNotNull();
        }
    }
}
