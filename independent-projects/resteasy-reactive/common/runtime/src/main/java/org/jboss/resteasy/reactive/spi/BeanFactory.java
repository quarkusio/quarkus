package org.jboss.resteasy.reactive.spi;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

public interface BeanFactory<T> {

    /**
     * Creates an endpoint instance outside the scope of a request
     */
    BeanInstance<T> createInstance();

    interface BeanInstance<T> extends AutoCloseable {

        T getInstance();

        void close();

        class ClosingTask<T> implements Closeable {
            private final Collection<BeanInstance<T>> instances;

            public ClosingTask(Collection<BeanInstance<T>> instances) {
                this.instances = instances;
            }

            @Override
            public void close() throws IOException {
                for (BeanFactory.BeanInstance<T> i : instances) {
                    i.close();
                }
            }
        }
    }

}
