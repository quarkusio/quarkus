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
        if (e instanceof Member) {
            return new IllegalArgumentException(msg + " at " + e + " of " + ((Member) e).getEnclosingDefinition());
        } else if (e instanceof Parameter) {
            return new IllegalArgumentException(msg + " at " + e + " of " + ((Parameter) e).getDeclaringExecutable() + " of "
                    + ((Parameter) e).getDeclaringExecutable().getDeclaringClass());
        } else {
            return new IllegalArgumentException(msg + " at " + e);
        }
    }

    public static abstract class Member {
        public abstract Definition getEnclosingDefinition();
    }
}
