package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.spi.CreationalContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SyntheticBeanCollisionTest {

    public static volatile boolean beanDestroyerInvoked = false;

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new TestRegistrar()).shouldFail().build();

    @Test
    public void testFailure() {
        assertNotNull(container.getFailure());
        assertTrue(container.getFailure().getMessage().contains("A synthetic bean with identifier"),
                container.getFailure().getMessage());
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext context) {
            context.configure(String.class).unremovable().types(String.class).param("name", "Frantisek")
                    .creator(StringCreator.class).done();

            context.configure(String.class).unremovable().types(String.class).param("name", "Martin")
                    .creator(StringCreator.class).done();
        }

    }

    public static class StringCreator implements BeanCreator<String> {

        @Override
        public String create(CreationalContext<String> creationalContext, Map<String, Object> params) {
            return "Hello " + params.get("name") + "!";
        }

    }

}
