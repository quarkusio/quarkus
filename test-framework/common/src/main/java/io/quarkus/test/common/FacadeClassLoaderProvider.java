package io.quarkus.test.common;

/**
 * This internal SPI is used by {@code io.quarkus.test.junit.classloading.FacadeClassLoader} from quarkus-junit5 to extend its
 * functionality.
 */
public interface FacadeClassLoaderProvider {

    /**
     * @param name The binary name of a class
     * @param parent
     * @return the class loader or null if no dedicated CL exists for the given class
     */
    ClassLoader getClassLoader(String name, ClassLoader parent);

}
