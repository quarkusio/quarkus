package io.quarkus.grpc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import io.grpc.ClientInterceptor;

/**
 * Registers a {@link ClientInterceptor} for an injected gRPC client.
 *
 * @see GlobalInterceptor
 */
@Qualifier
@Repeatable(RegisterClientInterceptor.List.class)
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface RegisterClientInterceptor {

    Class<? extends ClientInterceptor> value();

    @Target({ FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface List {

        RegisterClientInterceptor[] value();

    }

    final class Literal extends AnnotationLiteral<RegisterClientInterceptor> implements RegisterClientInterceptor {

        private static final long serialVersionUID = 1L;

        private final Class<? extends ClientInterceptor> value;

        public static Literal of(Class<? extends ClientInterceptor> value) {
            return new Literal(value);
        }

        private Literal(Class<? extends ClientInterceptor> value) {
            this.value = value;
        }

        public Class<? extends ClientInterceptor> value() {
            return value;
        }
    }
}
