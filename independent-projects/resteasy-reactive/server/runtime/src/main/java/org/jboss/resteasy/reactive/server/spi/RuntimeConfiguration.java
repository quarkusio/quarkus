package org.jboss.resteasy.reactive.server.spi;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.OptionalLong;

public interface RuntimeConfiguration {

    Duration readTimeout();

    Body body();

    Limits limits();

    interface Body {

        boolean deleteUploadedFilesOnEnd();

        String uploadsDirectory();

        Charset defaultCharset();

        MultiPart multiPart();

        interface MultiPart {
            List<String> fileContentTypes();
        }
    }

    interface Limits {
        OptionalLong maxBodySize();

        long maxFormAttributeSize();

        int maxParameters();
    }
}
