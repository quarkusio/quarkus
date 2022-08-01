package io.quarkus.arc.test.buildextension.observers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SyntheticObserverErrorTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .observerRegistrars(new ObserverRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure().observedType(String.class).notify(mc -> {
                        mc.returnValue(null);
                    }).done();

                    context.configure().observedType(String.class).notify(mc -> {
                        mc.returnValue(null);
                    }).done();
                }
            }).shouldFail().build();

    @Test
    public void testSyntheticObserver() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof IllegalStateException);
    }

}
