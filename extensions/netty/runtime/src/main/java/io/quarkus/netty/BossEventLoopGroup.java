package io.quarkus.netty;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface BossEventLoopGroup {

    /**
     * Supports inline instantiation of this qualifier.
     */
    public static final class Literal extends AnnotationLiteral<BossEventLoopGroup> implements BossEventLoopGroup {

        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;

    }
}
