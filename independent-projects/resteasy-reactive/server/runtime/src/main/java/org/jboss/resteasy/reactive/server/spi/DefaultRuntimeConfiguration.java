package org.jboss.resteasy.reactive.server.spi;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Optional;

public class DefaultRuntimeConfiguration implements RuntimeConfiguration {
    final Duration readTimeout;
    private final Body body;
    private final Limits limits;

    public DefaultRuntimeConfiguration(Duration readTimeout, boolean deleteUploadedFilesOnEnd, String uploadsDirectory,
            Charset defaultCharset, Optional<Long> maxBodySize, long maxFormAttributeSize) {
        this.readTimeout = readTimeout;
        body = new Body() {
            @Override
            public boolean deleteUploadedFilesOnEnd() {
                return deleteUploadedFilesOnEnd;
            }

            @Override
            public String uploadsDirectory() {
                return uploadsDirectory;
            }

            @Override
            public Charset defaultCharset() {
                return defaultCharset;
            }
        };
        limits = new Limits() {
            @Override
            public Optional<Long> maxBodySize() {
                return maxBodySize;
            }

            @Override
            public long maxFormAttributeSize() {
                return maxFormAttributeSize;
            }
        };
    }

    @Override
    public Duration readTimeout() {
        return readTimeout;
    }

    @Override
    public Body body() {
        return body;
    }

    @Override
    public Limits limits() {
        return limits;
    }
}
