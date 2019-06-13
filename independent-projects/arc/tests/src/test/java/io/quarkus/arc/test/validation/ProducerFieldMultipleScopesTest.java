package io.quarkus.arc.test.validation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DefinitionException;
import org.junit.Rule;
import org.junit.Test;

public class ProducerFieldMultipleScopesTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Alpha.class).shouldFail().build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
    }

    @Dependent
    static class Alpha {

        @Produces
        @ApplicationScoped
        @RequestScoped
        List<String> produced;

    }

}
