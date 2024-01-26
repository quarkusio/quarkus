package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.arc.All;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.beans.Bravo;
import io.quarkus.test.component.beans.Delta;
import io.quarkus.test.component.beans.SimpleQualifier;

@QuarkusComponentTest
public class ListAllMockTest {

    @Inject
    ListAllComponent component;

    @InjectMock
    Delta delta;

    @Inject
    @All
    List<Delta> deltas;

    @InjectMock
    @SimpleQualifier
    Bravo bravo;

    @Test
    public void testMock() {
        Mockito.when(delta.ping()).thenReturn(false);
        Mockito.when(bravo.ping()).thenReturn("ok");
        assertFalse(component.ping());
        assertEquals(1, component.bravos.size());
        assertEquals("ok", component.bravos.get(0).ping());
        assertEquals(deltas.get(0).ping(), component.ping());
    }

    @Singleton
    static class ListAllComponent {

        @All
        List<Delta> deltas;

        @All
        @SimpleQualifier
        List<Bravo> bravos;

        boolean ping() {
            return deltas.get(0).ping();
        }

    }
}
