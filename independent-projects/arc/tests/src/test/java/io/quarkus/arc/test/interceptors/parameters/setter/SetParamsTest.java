package io.quarkus.arc.test.interceptors.parameters.setter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class SetParamsTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(TheBean.class, Setter.class, FirstSetterInterceptor.class,
            SecondSetterInterceptor.class);

    @Test
    public void testSetParameters() {
        assertEquals("second", Arc.container().instance(TheBean.class).get().foo("bar"));
    }
}
