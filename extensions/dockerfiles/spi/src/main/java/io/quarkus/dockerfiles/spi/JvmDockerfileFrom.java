package io.quarkus.dockerfiles.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public interface JvmDockerfileFrom {

    String getFrom();

    public static final class Selected extends SimpleBuildItem implements JvmDockerfileFrom {
        private final String from;

        public Selected(String from) {
            this.from = from;
        }

        public String getFrom() {
            return from;
        }
    }

    public static final class Effective extends SimpleBuildItem implements JvmDockerfileFrom {
        private final String from;

        public Effective(String from) {
            this.from = from;
        }

        public String getFrom() {
            return from;
        }
    }
}
