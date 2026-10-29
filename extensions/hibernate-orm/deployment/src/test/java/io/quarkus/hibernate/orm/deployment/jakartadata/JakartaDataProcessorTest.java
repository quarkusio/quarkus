package io.quarkus.hibernate.orm.deployment.jakartadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Set;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem;

class JakartaDataProcessorTest {

    private final JakartaDataProcessor processor = new JakartaDataProcessor();

    @Test
    void producesNothingWhenSecurityIsAbsent() {
        var produced = new ArrayList<SecuredInterfaceAnnotationBuildItem>();

        processor.registerJakartaDataRepositorySecurityAnnotations(new Capabilities(Set.of()), produced::add);

        assertThat(produced).isEmpty();
    }

    @Test
    void registersTheRepositoryTypeAndEveryRepositoryMethodAnnotationWhenSecurityIsPresent() {
        var produced = new ArrayList<SecuredInterfaceAnnotationBuildItem>();

        processor.registerJakartaDataRepositorySecurityAnnotations(
                new Capabilities(Set.of(Capability.SECURITY)), produced::add);

        assertThat(produced)
                .extracting(item -> item.getAnnotationName().toString())
                .containsExactlyInAnyOrder(
                        "jakarta.data.repository.Repository",
                        Find.class.getName(),
                        HQL.class.getName(),
                        SQL.class.getName());
    }
}
