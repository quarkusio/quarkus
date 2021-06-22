package io.quarkus.grpc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * Qualifies an injected gRPC client.
 */
@Qualifier
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface GrpcClient {

    /**
     * Constant value for {@link #value()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * The name is used to configure the gRPC client, e.g. the location, TLS/SSL, etc.
     * 
     * @return the client name
     */
    String value() default ELEMENT_NAME;

    final class Literal extends AnnotationLiteral<GrpcClient> implements GrpcClient {

        private static final long serialVersionUID = 1L;

        private final String value;

        /**
         * Creates a new instance of {@link Literal}.
         *
         * @param value the name of the gRPC service, must not be {@code null}, must not be {@code blank}
         * @return the literal instance.
         */
        public static Literal of(String value) {
            return new Literal(value);
        }

        private Literal(String value) {
            this.value = value;
        }

        /**
         * @return the service name.
         */
        public String value() {
            return value;
        }
    }

}
