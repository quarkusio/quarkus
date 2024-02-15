package io.quarkus.test.component.paraminject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.Mockito;

import io.quarkus.arc.All;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.SkipInject;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.component.beans.Charlie;
import io.quarkus.test.component.beans.MyComponent;

@QuarkusComponentTest
public class ParameterInjectionTest {

    @ExtendWith(MyParamResolver.class)
    @TestConfigProperty(key = "foo", value = "BAZ")
    @Test
    public void testParamsInjection(
            // TestInfo should be ignored automatically
            TestInfo testInfo,
            // MyComponent is automatically a component
            MyComponent myComponent,
            // This would be normally resolved by QuarkusComponentTest but is annotated with @SkipInject
            @SkipInject MyComponent anotherComponent,
            // Inject unconfigured mock
            @InjectMock Charlie charlie,
            // Note that @SkipInject is redundant in this case because the Supplier interface cannot be used as a class-based bean
            // And so no matching bean exists
            @SkipInject Supplier<Object> shouldBeTrue,
            // @All List<> needs special handling
            @All List<MyComponent> allMyComponents) {
        Mockito.when(charlie.ping()).thenReturn("foo");
        assertNotNull(testInfo);
        assertEquals("foo and BAZ", myComponent.ping());
        assertNull(anotherComponent.getCharlie());
        assertEquals(1, allMyComponents.size());
        assertEquals(myComponent.ping(), allMyComponents.get(0).ping());
        assertEquals(Boolean.TRUE, shouldBeTrue.get());
    }

    public static class MyParamResolver implements ParameterResolver {

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return parameterContext.getParameter().getType().equals(Supplier.class)
                    || (parameterContext.getParameter().getType().equals(MyComponent.class)
                            && parameterContext.isAnnotated(SkipInject.class));
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            if (parameterContext.getParameter().getType().equals(Supplier.class)) {
                return new Supplier<Object>() {

                    @Override
                    public Object get() {
                        return Boolean.TRUE;
                    }
                };
            } else {
                return new MyComponent();
            }
        }

    }

}
