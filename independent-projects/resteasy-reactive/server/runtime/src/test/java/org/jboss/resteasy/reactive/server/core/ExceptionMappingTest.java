package org.jboss.resteasy.reactive.server.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import jakarta.ws.rs.Priorities;

import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.junit.jupiter.api.Test;

/**
 * Between the mappers registered for the same exception type, the one with the highest priority (the lowest value)
 * wins, and the one registered first wins when the priorities are equal.
 */
public class ExceptionMappingTest {

    private static final String EXCEPTION = IllegalStateException.class.getName();

    @Test
    public void testFirstRegisteredWinsWithTwoMappersOfEqualPriority() {
        ExceptionMapping mapping = new ExceptionMapping();
        mapping.addExceptionMapper(EXCEPTION, mapper("first", Priorities.USER, null));
        mapping.addExceptionMapper(EXCEPTION, mapper("second", Priorities.USER, null));
        assertEquals("first", effective(mapping));
    }

    @Test
    public void testFirstRegisteredWinsWithThreeMappersOfEqualPriority() {
        ExceptionMapping mapping = new ExceptionMapping();
        mapping.addExceptionMapper(EXCEPTION, mapper("first", Priorities.USER, null));
        mapping.addExceptionMapper(EXCEPTION, mapper("second", Priorities.USER, null));
        mapping.addExceptionMapper(EXCEPTION, mapper("third", Priorities.USER, null));
        assertEquals("first", effective(mapping));
    }

    @Test
    public void testHigherPriorityWinsRegardlessOfOrder() {
        ExceptionMapping mapping = new ExceptionMapping();
        mapping.addExceptionMapper(EXCEPTION, mapper("first", Priorities.USER, null));
        mapping.addExceptionMapper(EXCEPTION, mapper("second", Priorities.USER - 1, null));
        mapping.addExceptionMapper(EXCEPTION, mapper("third", Priorities.USER + 1, null));
        assertEquals("second", effective(mapping));

        mapping = new ExceptionMapping();
        mapping.addExceptionMapper(EXCEPTION, mapper("first", Priorities.USER + 1, null));
        mapping.addExceptionMapper(EXCEPTION, mapper("second", Priorities.USER, null));
        assertEquals("second", effective(mapping));
    }

    @Test
    public void testDiscardedMapperFallsBackToTheNextOne() {
        ExceptionMapping mapping = new ExceptionMapping();
        mapping.addExceptionMapper(EXCEPTION, mapper("discarded", Priorities.USER, () -> true));
        mapping.addExceptionMapper(EXCEPTION, mapper("second", Priorities.USER, null));
        assertEquals("second", effective(mapping));

        mapping = new ExceptionMapping();
        mapping.addExceptionMapper(EXCEPTION, mapper("kept", Priorities.USER, () -> false));
        mapping.addExceptionMapper(EXCEPTION, mapper("second", Priorities.USER, null));
        assertEquals("kept", effective(mapping));
    }

    @Test
    public void testConditionalMapperRegisteredAfterAnUnconditionalOneOfEqualPriorityIsNotUsed() {
        ExceptionMapping mapping = new ExceptionMapping();
        mapping.addExceptionMapper(EXCEPTION, mapper("first", Priorities.USER, null));
        mapping.addExceptionMapper(EXCEPTION, mapper("conditional", Priorities.USER, () -> false));
        assertEquals("first", effective(mapping));
    }

    private static String effective(ExceptionMapping mapping) {
        return mapping.effectiveMappers().get(EXCEPTION).getClassName();
    }

    private static ResourceExceptionMapper<Throwable> mapper(String className, int priority, Supplier<Boolean> discard) {
        ResourceExceptionMapper<Throwable> mapper = new ResourceExceptionMapper<>();
        mapper.setClassName(className);
        mapper.setPriority(priority);
        mapper.setDiscardAtRuntime(discard);
        return mapper;
    }
}
