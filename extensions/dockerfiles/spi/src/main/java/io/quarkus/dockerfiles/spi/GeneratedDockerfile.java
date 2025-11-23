package io.quarkus.dockerfiles.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public interface GeneratedDockerfile {

    String getContent();

    public static final class Jvm extends SimpleBuildItem implements GeneratedDockerfile {
        private final String content;

        public Jvm(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    public static final class Native extends SimpleBuildItem implements GeneratedDockerfile {
        private final String content;

        public Native(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }
}
