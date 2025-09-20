package io.quarkus.test.common;

/**
 * This SPI is used by FacadeClassLoader from quarkus-junit5 to extend its functionality.
 */
public interface FacadeClassLoaders {

    /**
     * @param name The binary name of a class
     * @param parent
     * @return the class loader or null if no special CL exists for the given class
     */
    ClassLoader getClassLoader(String name, ClassLoader parent);

}
