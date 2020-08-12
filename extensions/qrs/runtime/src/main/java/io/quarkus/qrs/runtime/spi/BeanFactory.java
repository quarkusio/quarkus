package io.quarkus.qrs.runtime.spi;

public interface BeanFactory<T> {

    /**
     * Creates an endpoint instance outside the scope of a request
     */
    BeanInstance<T> createInstance();

    interface BeanInstance<T> extends AutoCloseable {

        T getInstance();

        void close();
    }

}
