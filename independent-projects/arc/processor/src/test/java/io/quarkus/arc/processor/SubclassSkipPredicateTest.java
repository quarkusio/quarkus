package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.processor.Methods.SubclassSkipPredicate;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;

public class SubclassSkipPredicateTest {

    @Test
    public void testPredicate() throws IOException {
        IndexView index = Basics.index(Base.class, Submarine.class, Long.class, Number.class);
        AssignabilityCheck assignabilityCheck = new AssignabilityCheck(index, null);
        SubclassSkipPredicate predicate = new SubclassSkipPredicate(assignabilityCheck::isAssignableFrom, null);

        ClassInfo submarineClass = index.getClassByName(DotName.createSimple(Submarine.class.getName()));
        predicate.startProcessing(submarineClass, submarineClass);

        List<MethodInfo> echos = submarineClass.methods().stream().filter(m -> m.name().equals("echo"))
                .collect(Collectors.toList());
        assertEquals(2, echos.size());
        assertPredicate(echos, predicate);

        List<MethodInfo> getNames = submarineClass.methods().stream().filter(m -> m.name().equals("getName"))
                .collect(Collectors.toList());
        assertEquals(2, getNames.size());
        assertPredicate(getNames, predicate);

        predicate.methodsProcessed();
    }

    private void assertPredicate(List<MethodInfo> methods, SubclassSkipPredicate predicate) {
        for (MethodInfo method : methods) {
            if (Methods.isBridge(method)) {
                // Bridge method with impl
                assertTrue(predicate.test(method));
            } else {
                assertFalse(predicate.test(method));
            }
        }
    }

    static class Base<T extends Number, UNUSED> {

        String echo(T payload) {
            return payload.toString().toUpperCase();
        }

        T getName() {
            return null;
        }

    }

    @ApplicationScoped
    static class Submarine extends Base<Long, Boolean> {

        @Override
        String echo(Long payload) {
            return payload.toString();
        }

        @Override
        Long getName() {
            return 10l;
        }

    }

}
