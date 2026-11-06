package io.quarkus.hibernate.orm.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class HibernateOrmProcessorTest {

    private final HibernateOrmProcessor processor = new HibernateOrmProcessor();

    @Test
    void registerValidatorBeans() {
        List<ReflectiveClassBuildItem> reflection = new ArrayList<>();

        processor.enrollBeanValidationTypeSafeActivatorForReflection(new Capabilities(Set.of()), reflection::add);
        assertThat(reflection).isEmpty();

        processor.enrollBeanValidationTypeSafeActivatorForReflection(
                new Capabilities(Set.of(Capability.HIBERNATE_VALIDATOR)), reflection::add);
        assertThat(reflection).hasSize(2);
        assertThat(reflection).flatExtracting(ReflectiveClassBuildItem::getClassNames).containsExactlyInAnyOrder(
                "org.hibernate.boot.beanvalidation.TypeSafeActivator", BeanValidationIntegrator.JAKARTA_BV_CHECK_CLASS);
    }

}
