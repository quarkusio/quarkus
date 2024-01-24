package io.quarkus.test.component.paraminject;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.SkipInject;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.component.beans.MyComponent;

@QuarkusComponentTest
public class ParameterInjectionSkipOnMethodTest {

    @SkipInject
    @ExtendWith(MyParamResolver.class)
    @TestConfigProperty(key = "foo", value = "BAZ")
    @Test
    public void testInjectionSkipped(MyComponent myComponent, MyComponent anotherComponent) {
        // MyComponent is resolved by MyParamResolver
        assertNull(myComponent.getCharlie());
        assertNull(anotherComponent.getCharlie());
    }

    public static class MyParamResolver implements ParameterResolver {

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return parameterContext.getParameter().getType().equals(MyComponent.class);
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return new MyComponent();
        }

    }

}
