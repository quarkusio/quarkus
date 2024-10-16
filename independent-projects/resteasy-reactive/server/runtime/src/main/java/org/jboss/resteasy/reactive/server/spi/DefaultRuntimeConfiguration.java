package org.jboss.resteasy.reactive.server.spi;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class DefaultRuntimeConfiguration implements RuntimeConfiguration {
    final Duration readTimeout;
    private final Body body;
    private final Limits limits;

    public DefaultRuntimeConfiguration(Duration readTimeout, boolean deleteUploadedFilesOnEnd, String uploadsDirectory,
            List<String> fileContentTypes, Charset defaultCharset, Optional<Long> maxBodySize, long maxFormAttributeSize,
            int maxParameters) {
        this.readTimeout = readTimeout;
        body = new Body() {
            Body.MultiPart multiPart = new Body.MultiPart() {
                @Override
                public List<String> fileContentTypes() {
                    return fileContentTypes;
                }
            };

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

            @Override
            public MultiPart multiPart() {
                return multiPart;
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

            @Override
            public int maxParameters() {
                return maxParameters;
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
