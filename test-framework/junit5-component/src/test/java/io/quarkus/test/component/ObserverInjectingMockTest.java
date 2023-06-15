package io.quarkus.test.component;

import static org.mockito.Mockito.times;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.beans.Delta;
import io.quarkus.test.component.beans.MyComponent;

public class ObserverInjectingMockTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = new QuarkusComponentTestExtension(MyComponent.class)
            .useDefaultConfigProperties();

    @Inject
    Event<Boolean> event;

    @InjectMock
    Delta delta;

    @Test
    public void testObserver() {
        event.fire(Boolean.TRUE);
        event.fire(Boolean.FALSE);
        Mockito.verify(delta, times(2)).onBoolean();
    }

}
