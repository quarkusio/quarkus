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

            /**
             * File parts up to this size are kept in memory rather than written to a file; {@code 0} writes every
             * file part to a file
             */
            long fileSizeThreshold();
        }
    }

    interface Limits {
        OptionalLong maxBodySize();

        long maxFormAttributeSize();

        int maxParameters();

        int maxMultipartPartHeaderSize();

        int maxMultipartHeaderCount();
    }
}
