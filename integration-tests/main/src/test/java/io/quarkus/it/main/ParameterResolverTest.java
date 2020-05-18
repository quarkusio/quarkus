package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.quarkus.it.arc.UnusedBean;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ExtendWith(ParameterResolverTest.UnusedBeanDummyInputResolver.class)
public class ParameterResolverTest {

    @Inject
    UnusedBean unusedBean;

    @Test
    public void testParameterResolver(UnusedBean.DummyInput dummyInput) {
        UnusedBean.DummyResult dummyResult = unusedBean.dummy(dummyInput);
        assertEquals("whatever/6", dummyResult.getResult());
    }

    public static class UnusedBeanDummyInputResolver implements ParameterResolver {

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return UnusedBean.DummyInput.class.getName().equals(parameterContext.getParameter().getType().getName());
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext,
                ExtensionContext extensionContext) throws ParameterResolutionException {
            return new UnusedBean.DummyInput("whatever", new UnusedBean.NestedDummyInput(Arrays.asList(1, 2, 3)));
        }
    }

}
