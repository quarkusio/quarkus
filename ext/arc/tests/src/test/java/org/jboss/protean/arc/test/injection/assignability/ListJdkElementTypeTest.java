package org.jboss.protean.arc.test.injection.assignability;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class ListJdkElementTypeTest {

    @Rule
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
