package org.jboss.resteasy.reactive.common.processor;

/**
 * Used to determine the java version of the compiled Java code
 */
public interface TargetJavaVersion {

    Status isJava19OrHigher();

    enum Status {
        TRUE,
        FALSE,
        UNKNOWN
    }

    final class Unknown implements TargetJavaVersion {

        public static final Unknown INSTANCE = new Unknown();

        Unknown() {
        }

        @Override
        public Status isJava19OrHigher() {
            return Status.UNKNOWN;
        }
    }
}
