package io.quarkus.hibernate.orm.deployment.persistenceunit;

import static io.quarkus.hibernate.orm.deployment.ProcessorTestStubs.persistenceUnitDefinition;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.deployment.spi.PersistenceUnitDefinedBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;

class PersistenceUnitProcessorTest {

    private final PersistenceUnitProcessor processor = new PersistenceUnitProcessor();

    @Test
    void aggregatesBlockingAndReactiveDefinitionsOfTheSamePersistenceUnit() {
        var definitions = List.of(
                persistenceUnitDefinition("inventory", ProgrammingParadigm.BLOCKING, "inventory-ds"),
                persistenceUnitDefinition("inventory", ProgrammingParadigm.REACTIVE, "inventory-ds"),
                persistenceUnitDefinition("users", ProgrammingParadigm.BLOCKING, "users-ds"));

        var aggregatedPersistenceUnits = new ArrayList<PersistenceUnitDefinedBuildItem>();

        processor.aggregateDefinedPersistenceUnits(definitions, aggregatedPersistenceUnits::add);

        assertThat(aggregatedPersistenceUnits).hasSize(2);

        var inventory = aggregatedPersistenceUnits.getFirst();
        assertThat(inventory.getPersistenceUnitName()).isEqualTo("inventory");
        assertThat(inventory.getDataSourceName()).contains("inventory-ds");
        assertThat(inventory.getParadigms())
                .containsExactly(ProgrammingParadigm.BLOCKING, ProgrammingParadigm.REACTIVE);

        var users = aggregatedPersistenceUnits.get(1);
        assertThat(users.getPersistenceUnitName()).isEqualTo("users");
        assertThat(users.getDataSourceName()).contains("users-ds");
        assertThat(users.getParadigms()).containsExactly(ProgrammingParadigm.BLOCKING);
    }

}
