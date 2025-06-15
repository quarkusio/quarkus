package io.quarkus.quartz.runtime.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * See the javadoc in {@link QuarkusObjectInputStream#resolveClass(ObjectStreamClass)}
 */
class QuarkusObjectInputStream extends ObjectInputStream {
    public QuarkusObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    /**
     * We override the {@link ObjectInputStream#resolveClass(ObjectStreamClass)} method to workaround a class loading
     * issue in Test & Dev mode. This is because, the implementation of this method in ObjectInputStream returns the
     * result of calling Class.forName(desc.getName(), false, loader) where loader is the first class loader on the
     * current thread's stack (starting from the currently executing method) that is neither the platform class loader
     * nor its ancestor; otherwise, loader is the platform class loader. That classloader happens to the Base Runtime
     * QuarkusClassLoader in Test and Dev mode which was causing {@link ClassNotFoundException} when loading
     * user/application classes.
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();
        try {
            // uses the TCCL to workaround CNFE encountered in test & dev mode
            return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException ex) {
            return super.resolveClass(desc);
        }
    }
}
