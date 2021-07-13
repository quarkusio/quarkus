package org.mockito.configuration;

import java.lang.reflect.InvocationTargetException;

import org.mockito.stubbing.Answer;

import io.quarkus.test.junit.mockito.internal.MutinyAnswer;

public class MockitoConfiguration extends DefaultMockitoConfiguration {

    @SuppressWarnings("unchecked")
    @Override
    public Answer<Object> getDefaultAnswer() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            // we need to load it from the TCCL (QuarkusClassLoader) instead of our class loader (JUnit CL)
            Class<?> mutinyAnswer = cl.loadClass(MutinyAnswer.class.getName());
            return (Answer<Object>) mutinyAnswer.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | SecurityException | IllegalArgumentException | IllegalAccessException
                | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Failed to load MutinyAnswer from the TCCL", e);
        }
    }
}
