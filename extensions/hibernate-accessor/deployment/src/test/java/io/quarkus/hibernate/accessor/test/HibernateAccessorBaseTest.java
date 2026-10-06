package io.quarkus.hibernate.accessor.test;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;

abstract class HibernateAccessorBaseTest {

    protected ValueReader<?> reader(Class<?> klass, String field) throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        return factory.valueReader(klass.getDeclaredField(field));
    }

    protected ValueWriter writer(Class<?> klass, String field) throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        return factory.valueWriter(klass.getDeclaredField(field));
    }

    protected ValueReader<?> readerMethod(Class<?> klass, String method) throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        return factory.valueReader(klass.getDeclaredMethod(method));
    }

    protected ValueWriter writerMethod(Class<?> klass, String method, Class<?> parameter) throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        return factory.valueWriter(klass.getDeclaredMethod(method, parameter));
    }

    protected AccessorFactory loadGeneratedFactory() throws Exception {
        Class<?> factoryClass = Thread.currentThread().getContextClassLoader()
                .loadClass("io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorFactory");
        return (AccessorFactory) factoryClass.getMethod("create").invoke(null);
    }
}
