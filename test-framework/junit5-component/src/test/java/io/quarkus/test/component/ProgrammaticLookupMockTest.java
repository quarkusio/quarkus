package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.beans.Delta;

public class ProgrammaticLookupMockTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = new QuarkusComponentTestExtension(
            ProgrammaticLookComponent.class);

    @Inject
    ProgrammaticLookComponent component;

    @InjectMock
    Delta delta;

    @Test
    public void testMock() {
        Mockito.when(delta.ping()).thenReturn(false);
        assertFalse(component.ping());
    }

    @Singleton
    static class ProgrammaticLookComponent {

        @Inject
        Instance<Delta> delta;

        boolean ping() {
            return delta.get().ping();
        }

    }
}
