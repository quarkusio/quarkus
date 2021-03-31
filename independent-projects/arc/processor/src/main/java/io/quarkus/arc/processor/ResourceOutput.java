package io.quarkus.arc.processor;

import java.io.File;
import java.io.IOException;

/**
 * Represents a generated resource.
 *
 * @author Martin Kouba
 */
public interface ResourceOutput {

    void writeResource(Resource resource) throws IOException;

    interface Resource {

        boolean isApplicationClass();

        File writeTo(File directory) throws IOException;

        byte[] getData();

        /**
         * <pre>
         * com/foo/MyBean
         * com/foo/MyBean$Bar
         * io.quarkus.arc.BeanProvider
         * </pre>
         *
         * @return the name
         */
        String getName();

        /**
         * <pre>
         * com.foo.MyBean
         * </pre>
         *
         * @return the fully qualified name as defined in JLS
         */
        default String getFullyQualifiedName() {
            if (Type.JAVA_CLASS.equals(getType()) || Type.JAVA_SOURCE.equals(getType())) {
                return getName().replace('/', '.');
            }
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @see Type
         * @return the type
         */
        Type getType();

        /**
         *
         * @see SpecialType
         * @return the special type or null
         */
        SpecialType getSpecialType();

        /**
         * 
         * @return the textual representation of generated code
         */
        String getSource();

        enum Type {
            JAVA_CLASS,
            JAVA_SOURCE,
            SERVICE_PROVIDER,
        }

        enum SpecialType {
            BEAN,
            INTERCEPTOR_BEAN,
            DECORATOR_BEAN,
            OBSERVER;
        }

    }

}
