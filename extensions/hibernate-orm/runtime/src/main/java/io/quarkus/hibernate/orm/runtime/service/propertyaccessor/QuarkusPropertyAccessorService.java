package io.quarkus.hibernate.orm.runtime.service.propertyaccessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.Instantiator;
import org.hibernate.accessor.MultiValueReader;
import org.hibernate.accessor.MultiValueWriter;
import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.hibernate.property.access.spi.PropertyAccessorService;

final class QuarkusPropertyAccessorService implements PropertyAccessorService {

    static final QuarkusPropertyAccessorService INSTANCE = new QuarkusPropertyAccessorService();

    private final AccessorFactory accessorFactory = new ReflectionAccessorFactory();

    private QuarkusPropertyAccessorService() {
    }

    @Override
    public AccessorFactory hibernateAccessorFactory() {
        return accessorFactory;
    }

    private static final class ReflectionAccessorFactory implements AccessorFactory {

        private final AccessorFactory delegate = AccessorFactory.reflection();

        @Override
        public <T> Instantiator<T> instantiator(Constructor<T> constructor) {
            return delegate.instantiator(constructor);
        }

        @Override
        public ValueReader<?> valueReader(Field field) {
            return delegate.valueReader(field);
        }

        @Override
        public ValueReader<?> valueReader(Method method) {
            return delegate.valueReader(method);
        }

        @Override
        public ValueWriter valueWriter(Field field) {
            return delegate.valueWriter(field);
        }

        @Override
        public ValueWriter valueWriter(Method method) {
            return delegate.valueWriter(method);
        }

        @Override
        public MultiValueReader multiValueReader(Class<?> declaringClass, Member... members) {
            // The reflection bulk reader does not preserve UNFETCHED_PROPERTY for lazy fields.
            return null;
        }

        @Override
        public MultiValueWriter multiValueWriter(Class<?> declaringClass, Member... members) {
            // The reflection bulk writer cannot assign UNFETCHED_PROPERTY to lazy fields.
            return null;
        }
    }
}
