package io.quarkus.test.junit.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;

/**
 * This callback verifies that {@code @io.quarkus.test.InjectMock} is not declared on a field of a {@code @QuarkusTest}
 * unless the {@code quarkus-junit5-mockito} is present.
 */
public class VerifyMockitoMocksCallback implements QuarkusTestAfterConstructCallback {

    @Override
    public void afterConstruct(Object testInstance) {
        Class<? extends Annotation> deprecatedInjectMock = loadDeprecatedInjectMock();
        if (deprecatedInjectMock == null) {
            List<Field> injectMockFields = new ArrayList<>();
            Class<?> current = testInstance.getClass();
            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(InjectMock.class)) {
                        injectMockFields.add(field);
                    }
                }
                current = current.getSuperclass();
            }
            if (!injectMockFields.isEmpty()) {
                throw new IllegalStateException(
                        "@io.quarkus.test.InjectMock declared on one or more fields of a @QuarkusTest but the quarkus-junit5-mockito dependency is not present: "
                                + injectMockFields.stream().map(f -> "/n/t- " + f.toString()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> loadDeprecatedInjectMock() {
        try {
            return (Class<? extends Annotation>) Class.forName("io.quarkus.test.junit.mockito.InjectMock");
        } catch (Throwable e) {
            return null;
        }
    }
}
