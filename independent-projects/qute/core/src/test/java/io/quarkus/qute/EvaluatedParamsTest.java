package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

public class EvaluatedParamsTest {

    @Test
    public void testParameterTypesMatch() throws InterruptedException, ExecutionException {
        assertTrue(new EvaluatedParams(null, new Supplier[] { CompletedStage.of("Foo") })
                .parameterTypesMatch(false, new Class<?>[] { String.class }));
        assertTrue(new EvaluatedParams(null, new Supplier[] { CompletedStage.of(10) })
                .parameterTypesMatch(false, new Class<?>[] { Number.class }));
        assertTrue(new EvaluatedParams(null,
                new Supplier[] { CompletedStage.of(10), CompletedStage.of("Foo"),
                        CompletedStage.of("Bar") })
                                .parameterTypesMatch(true, new Class<?>[] { Integer.class, Object[].class }));
        // varargs may be empty
        assertTrue(new EvaluatedParams(null,
                new Supplier[] { CompletedStage.of(10) })
                        .parameterTypesMatch(true, new Class<?>[] { Integer.class, Object[].class }));
        assertFalse(new EvaluatedParams(null,
                new Supplier[] { CompletedStage.of("str") })
                        .parameterTypesMatch(true, new Class[] { Locale.class, Object[].class }));

        // Integer,String does not match Integer,Object[] 
        assertFalse(new EvaluatedParams(null,
                new Supplier[] { CompletedStage.of(10), CompletedStage.of("Foo") })
                        .parameterTypesMatch(false, new Class<?>[] { Integer.class, Object[].class }));

    }

}
