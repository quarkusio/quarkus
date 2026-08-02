package io.quarkus.data.hibernate.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.data.hibernate.deployment.test.MyEntity;
import io.quarkus.data.hibernate.deployment.test.MyReactiveEntity;
import io.quarkus.deployment.Capabilities;

public class ReactiveQuarkusDataWithoutHibernateReactiveTest {

    @Test
    public void reactiveEntityWithoutHibernateReactiveIsRejected() {
        Capabilities capabilities = new Capabilities(Collections.emptySet());
        Set<String> offendingTypes = ReactiveQuarkusDataValidator.findOffendingReactiveQuarkusDataTypes(
                ReactiveQuarkusDataValidatorTestHelper.indexOf(MyReactiveEntity.class), capabilities);

        assertThat(offendingTypes).contains(MyReactiveEntity.class.getName());
    }

    @Test
    public void blockingEntityWithoutHibernateReactiveIsAllowed() {
        Capabilities capabilities = new Capabilities(Collections.emptySet());
        Set<String> offendingTypes = ReactiveQuarkusDataValidator.findOffendingReactiveQuarkusDataTypes(
                ReactiveQuarkusDataValidatorTestHelper.indexOf(MyEntity.class), capabilities);

        assertThat(offendingTypes).isEmpty();
    }
}
