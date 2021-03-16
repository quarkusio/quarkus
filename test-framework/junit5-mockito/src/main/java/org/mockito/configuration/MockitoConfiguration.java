package org.mockito.configuration;

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
            return (Answer<Object>) mutinyAnswer.newInstance();
        } catch (ClassNotFoundException | SecurityException | IllegalArgumentException | IllegalAccessException
                | InstantiationException e) {
            throw new RuntimeException("Failed to load MutinyAnswer from the TCCL", e);
        }
    }
}
