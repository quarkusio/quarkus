package org.jboss.resteasy.reactive.server.spi;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Optional;

public interface RuntimeConfiguration {

    Duration readTimeout();

    Body body();

    Limits limits();

    interface Body {

        boolean deleteUploadedFilesOnEnd();

        String uploadsDirectory();

        Charset defaultCharset();
    }

    interface Limits {
        Optional<Long> maxBodySize();

        long maxFormAttributeSize();
    }
}
