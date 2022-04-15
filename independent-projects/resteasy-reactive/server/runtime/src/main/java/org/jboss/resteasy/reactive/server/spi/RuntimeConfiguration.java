package org.jboss.resteasy.reactive.server.spi;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public interface RuntimeConfiguration {

    Duration readTimeout();

    Body body();

    Limits limits();

    boolean enableCompression();

    Set<String> compressMediaTypes();

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
