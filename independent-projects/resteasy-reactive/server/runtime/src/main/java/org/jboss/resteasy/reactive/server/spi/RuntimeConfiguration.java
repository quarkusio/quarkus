package org.jboss.resteasy.reactive.server.spi;

import java.time.Duration;
import java.util.Optional;

public interface RuntimeConfiguration {

    Duration readTimeout();

    Body body();

    Limits limits();

    interface Body {

        boolean deleteUploadedFilesOnEnd();

        String uploadsDirectory();
    }

    interface Limits {
        Optional<Long> maxBodySize();

        long maxFormAttributeSize();
    }
}
