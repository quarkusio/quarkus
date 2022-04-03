package io.quarkus.resteasy.reactive.server.runtime;

import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfigurableServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfiguration;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.vertx.http.runtime.HttpCompression;

public class ResteasyReactiveCompressionHandler implements ServerRestHandler, RuntimeConfigurableServerRestHandler {

    private HttpCompression compression;
    private volatile boolean enableCompression;
    private volatile Set<String> compressMediaTypes;

    public ResteasyReactiveCompressionHandler() {
    }

    public HttpCompression getCompression() {
        return compression;
    }

    public void setCompression(HttpCompression compression) {
        this.compression = compression;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        if (enableCompression) {
            ServerHttpResponse response = requestContext.serverResponse();
            String contentEncoding = response.getResponseHeader(HttpHeaders.CONTENT_ENCODING);
            if (contentEncoding != null && io.vertx.core.http.HttpHeaders.IDENTITY.toString().equals(contentEncoding)) {
                switch (compression) {
                    case ON:
                        response.removeResponseHeader(HttpHeaders.CONTENT_ENCODING);
                        break;
                    case UNDEFINED:
                        MediaType contentType = requestContext.getResponseContentType().getMediaType();
                        if (contentType != null
                                && compressMediaTypes.contains(contentType.getType() + '/' + contentType.getSubtype())) {
                            response.removeResponseHeader(HttpHeaders.CONTENT_ENCODING);
                        }
                        break;
                    default:
                        // OFF - no action is needed because the "Content-Encoding: identity" header is set
                        break;
                }
            }
        }
    }

    @Override
    public void configure(RuntimeConfiguration configuration) {
        enableCompression = configuration.enableCompression();
        compressMediaTypes = configuration.compressMediaTypes();
    }
}
