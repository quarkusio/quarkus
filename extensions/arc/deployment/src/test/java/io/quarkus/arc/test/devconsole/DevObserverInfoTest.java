package io.quarkus.arc.test.devconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.deployment.devconsole.DevObserverInfo;
import io.quarkus.arc.deployment.devconsole.Name;

public class DevObserverInfoTest {

    @Test
    public void testCompare() {
        List<DevObserverInfo> observers = new ArrayList<>();
        // Synthetic non-app - should be last
        observers.add(new DevObserverInfo(false, null, null, new Name("Delta"),
                Collections.emptyList(), 0, false, Reception.ALWAYS, TransactionPhase.IN_PROGRESS));
        // App observers
        observers.add(new DevObserverInfo(true, new Name("Alpha"), "fooish", new Name("java.lang.String"),
                Collections.emptyList(), 0, false, Reception.ALWAYS, TransactionPhase.IN_PROGRESS));
        observers.add(new DevObserverInfo(true, new Name("Alpha"), "blabla", new Name("java.lang.String"),
                Collections.emptyList(), 1, false, Reception.ALWAYS, TransactionPhase.IN_PROGRESS));
        observers.add(new DevObserverInfo(true, null, null, new Name("Charlie"),
                Collections.emptyList(), 0, false, Reception.ALWAYS, TransactionPhase.IN_PROGRESS));
        observers.add(new DevObserverInfo(true, new Name("Bravo"), "hop", new Name("java.lang.String"),
                Collections.emptyList(), 0, false, Reception.IF_EXISTS, TransactionPhase.IN_PROGRESS));

        Collections.sort(observers);
        assertEquals("blabla", observers.get(0).getMethodName());
        assertEquals("Alpha", observers.get(0).getDeclaringClass().toString());
        assertEquals("fooish", observers.get(1).getMethodName());
        assertEquals("Alpha", observers.get(1).getDeclaringClass().toString());
        assertEquals("hop", observers.get(2).getMethodName());
        assertEquals("Bravo", observers.get(2).getDeclaringClass().toString());
        assertNull(observers.get(3).getMethodName());
        assertEquals("Charlie", observers.get(3).getObservedType().toString());
        assertEquals("Delta", observers.get(observers.size() - 1).getObservedType().toString());
        assertNull(observers.get(observers.size() - 1).getDeclaringClass());
    }

}
