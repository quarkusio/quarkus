package io.quarkus.arc.test.clientproxy.multiple.active.contexts;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class ClientProxyMultipleActiveContextsTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Foo.class)
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
        Foo foo = Arc.container().instance(Foo.class).get();
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(foo::ping)
                .withMessageContaining("More than one active context object for scope @CustomScoped");
    }

    @CustomScoped
    static class Foo {
        void ping() {
        }
    }
}
