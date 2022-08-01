package io.quarkus.arc.test.index;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AdditionalIndexTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Client.class, SuperProducer.class, SuperClazz.class)
            // SubClazz is not part of the bean archive index
            .additionalClasses(SubClazz.class).build();

    @Test
    public void testTypesafeResolution() {
        Client client = Arc.container().instance(Client.class).get();
        // Test that the build does not fail and we're able to inject the producer
        assertTrue(client.list.isEmpty());
    }

    @Dependent
    static class Client {

        // This injection point should be satisfied by SuperProducer#produce()
        // because SubClazz extends SuperClazz
        @Inject
        List<SubClazz> list;
    }

    public static class SuperProducer {

        @Produces
        <T extends SuperClazz> List<T> produce() {
            return new ArrayList<>();
        }

    }

    public static class SuperClazz {

        public void ping() {
        }
    }

    public static class SubClazz extends SuperClazz {

    }

}
