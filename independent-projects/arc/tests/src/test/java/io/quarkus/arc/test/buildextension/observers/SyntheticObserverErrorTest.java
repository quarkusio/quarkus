package io.quarkus.arc.test.buildextension.observers;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticObserverErrorTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .observerRegistrars(new ObserverRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure().observedType(String.class).notify(og -> {
                        og.notifyMethod().return_();
                    }).done();

                    context.configure().observedType(String.class).notify(og -> {
                        og.notifyMethod().return_();
                    }).done();
                }
            }).shouldFail().build();

    @Test
    public void testSyntheticObserver() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(IllegalStateException.class, error);
    }

}
