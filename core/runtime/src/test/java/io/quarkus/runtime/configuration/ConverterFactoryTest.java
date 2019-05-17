package io.quarkus.runtime.configuration;

import static org.junit.Assert.assertSame;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

/**
 * Tests against {@link ConverterFactory}.
 */
public class ConverterFactoryTest {

    static class MyPojo {

        private final int number;

        public MyPojo(final int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }
    }

    static class MyPojoConverter implements Converter<MyPojo> {

        @Override
        public MyPojo convert(final String value) {
            return new MyPojo(Integer.valueOf(value));
        }
    }

    @Test
    public void testgetConverterType() {
        assertSame(MyPojo.class, ConverterFactory.getConverterType(new MyPojoConverter()));
    }
}
