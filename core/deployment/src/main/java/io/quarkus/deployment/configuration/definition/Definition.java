package io.quarkus.deployment.configuration.definition;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;

/**
 * A configuration definition. Definitions always contain links to the things they contain, but not to their own
 * containers.
 */
public abstract class Definition {
    Definition() {
    }

    public static abstract class Builder {
        Builder() {
        }

        public abstract Definition build();
    }

    static IllegalArgumentException reportError(AnnotatedElement e, String msg) {
        if (e instanceof Member member) {
            return new IllegalArgumentException(msg + " at " + e + " of " + member.getEnclosingDefinition());
        } else if (e instanceof Parameter parameter) {
            return new IllegalArgumentException(msg + " at " + e + " of " + parameter.getDeclaringExecutable() + " of "
                    + parameter.getDeclaringExecutable().getDeclaringClass());
        } else {
            return new IllegalArgumentException(msg + " at " + e);
        }
    }

    public static abstract class Member {
        public abstract Definition getEnclosingDefinition();
    }
}
