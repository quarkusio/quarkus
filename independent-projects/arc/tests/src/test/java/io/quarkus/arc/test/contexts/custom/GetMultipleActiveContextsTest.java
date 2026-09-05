package io.quarkus.arc.test.contexts.custom;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class GetMultipleActiveContextsTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .contextRegistrars(new ContextRegistrar() {
                @Override
                public void register(RegistrationContext registrationContext) {
                    registrationContext.configure(CustomScoped.class).contextClass(CustomContextImpl1.class).done();
                    registrationContext.configure(CustomScoped.class).contextClass(CustomContextImpl2.class).done();
                }
            })
            .build();

    @Test
    public void test() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> Arc.container().getActiveContext(CustomScoped.class))
                .withMessageContaining("More than one active context object for scope @CustomScoped");
    }
}
