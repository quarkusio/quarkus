package io.quarkus.arc.test.bean.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class BeanCreateErrorTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(CheckedExceptionBean.class, CheckedExceptionProducerBean.class,
            UncheckedExceptionBean.class, UncheckedExceptionProducerBean.class);

    @Test
    public void checkedExceptionWrapped() {
        // Test that the checked exception is wrapped

        CreationException exception = assertThrows(CreationException.class, () -> {
            Arc.container().instance(CheckedExceptionBean.class);
        });
        assertEquals("foo", exception.getCause().getMessage());

        exception = assertThrows(CreationException.class, () -> {
            Arc.container().instance(BigInteger.class);
        });
        assertEquals("bar", exception.getCause().getMessage());
    }

    @Test
    public void uncheckedExceptionNotWrapped() {
        // Test that the unchecked exception is _not_ wrapped

        assertThrows(IllegalArgumentException.class, () -> {
            Arc.container().instance(UncheckedExceptionBean.class);
        });

        assertThrows(IllegalStateException.class, () -> {
            Arc.container().instance(BigDecimal.class);
        });
    }

    @Dependent
    static class CheckedExceptionBean {
        @Inject
        public CheckedExceptionBean() throws Exception {
            throw new Exception("foo");
        }
    }

    @Dependent
    static class CheckedExceptionProducerBean {
        @Produces
        @Dependent
        BigInteger produce() throws Exception {
            throw new Exception("bar");
        }
    }

    @Dependent
    static class UncheckedExceptionBean {
        @Inject
        public UncheckedExceptionBean() {
            throw new IllegalArgumentException();
        }
    }

    @Dependent
    static class UncheckedExceptionProducerBean {
        @Produces
        @Dependent
        BigDecimal produce() {
            throw new IllegalStateException();
        }
    }
}
