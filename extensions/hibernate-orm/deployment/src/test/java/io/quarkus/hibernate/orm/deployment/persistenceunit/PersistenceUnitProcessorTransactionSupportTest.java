package io.quarkus.hibernate.orm.deployment.persistenceunit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.runtime.configuration.ConfigurationException;

class PersistenceUnitProcessorTransactionSupportTest {

    private final PersistenceUnitProcessor processor = new PersistenceUnitProcessor();

    @Test
    void reportsMissingJtaForBlockingOrm() {
        var errors = transactionErrors(new Capabilities(Set.of()));

        assertThat(errors).singleElement().satisfies(error -> assertThat(error.getValues()).singleElement()
                .isInstanceOfSatisfying(ConfigurationException.class,
                        exception -> assertThat(exception)
                                .hasMessage("The Hibernate ORM extension is only functional in a JTA environment.")));
    }

    @Test
    void acceptsEitherTransactionsOrReactiveOrm() {
        assertThat(transactionErrors(new Capabilities(Set.of(Capability.TRANSACTIONS)))).isEmpty();
        assertThat(transactionErrors(new Capabilities(Set.of(Capability.HIBERNATE_REACTIVE)))).isEmpty();
        assertThat(transactionErrors(new Capabilities(Set.of(Capability.TRANSACTIONS, Capability.HIBERNATE_REACTIVE))))
                .isEmpty();
    }

    private List<ValidationErrorBuildItem> transactionErrors(Capabilities capabilities) {
        var errors = new ArrayList<ValidationErrorBuildItem>();
        processor.checkTransactionsSupport(capabilities, errors::add);
        return errors;
    }
}
