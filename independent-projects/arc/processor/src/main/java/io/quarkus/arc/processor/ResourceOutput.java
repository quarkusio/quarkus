package io.quarkus.arc.processor;

import java.io.File;
import java.io.IOException;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

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

        /**
         * Returns the bootstrap methods declared by this class which are used by {@code invokedynamic}
         * instructions or dynamic constants in the generated code.
         * They have to be registered with GraalVM native image so that they are run at build time.
         *
         * @return the bootstrap methods of this class, never {@code null}
         */
        default List<BootstrapMethod> getBootstrapMethods() {
            return List.of();
        }

        /**
         * A bootstrap method declared by a generated class.
         *
         * @param name the method name
         * @param type the method type
         */
        record BootstrapMethod(String name, MethodTypeDesc type) {
        }

        enum Type {
            JAVA_CLASS,
            JAVA_SOURCE,
            SERVICE_PROVIDER
        }

        enum SpecialType {
            BEAN,
            INTERCEPTOR_BEAN,
            DECORATOR_BEAN,
            OBSERVER,
            CLIENT_PROXY,
            SUBCLASS,
            INVOKER,
        }

    }

}
