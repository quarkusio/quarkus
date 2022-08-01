package io.quarkus.arc.test.injection.assignability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.Collections;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ListJdkElementTypeTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(ListProducer.class, InjectList.class);

    @Test
    public void testInjection() {
        assertEquals(Integer.valueOf(1), Arc.container().instance(InjectList.class).get().getListOfNumbers().get(0));
    }

    @ApplicationScoped
    static class ListProducer {

        @Dependent
        @Produces
        List<Integer> produceListOfIntegers() {
            return Collections.singletonList(1);
        }

    }

    @ApplicationScoped
    static class InjectList {

        private List<? extends Number> listOfNumbers;

        @Inject
        private void setLists(List<? extends Number> listOfNumbers) {
            this.listOfNumbers = listOfNumbers;
        }

        List<? extends Number> getListOfNumbers() {
            return listOfNumbers;
        }

    }
}
