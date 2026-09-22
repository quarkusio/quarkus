package io.quarkus.hibernate.orm.runtime.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfigPersistenceUnit.DataManagementStrategy;
import io.quarkus.runtime.LaunchMode;

/**
 * The data init script is only executed by default in dev and test modes:
 * outside of them (the launch mode of a plain unit test is {@link LaunchMode#NORMAL}),
 * the default strategy is {@code none}, whatever the schema management strategy.
 */
class InitScriptSupportTest {

    @Test
    void defaultStrategy_outsideDevAndTestModes() {
        assertThat(LaunchMode.current()).isEqualTo(LaunchMode.NORMAL);
        assertThat(InitScriptSupport.defaultDataManagementStrategy()).isEqualTo(DataManagementStrategy.NONE);
    }
}
