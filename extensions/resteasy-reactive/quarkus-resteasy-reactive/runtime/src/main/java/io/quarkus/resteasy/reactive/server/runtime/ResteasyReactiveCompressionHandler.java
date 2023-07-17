package io.quarkus.resteasy.reactive.server.runtime;

import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.core.EncodedMediaType;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.vertx.http.runtime.HttpCompression;

public class ResteasyReactiveCompressionHandler implements ServerRestHandler {

    private HttpCompression compression;
    private Set<String> compressMediaTypes;
    private String produces;
    private volatile EncodedMediaType encodedProduces;

    public ResteasyReactiveCompressionHandler() {
    }

    public ResteasyReactiveCompressionHandler(Set<String> compressMediaTypes) {
        this.compressMediaTypes = compressMediaTypes;
    }

    public HttpCompression getCompression() {
        return compression;
    }

    public void setCompression(HttpCompression compression) {
        this.compression = compression;
    }

    public Set<String> getCompressMediaTypes() {
        return compressMediaTypes;
    }

    public void setCompressMediaTypes(Set<String> compressMediaTypes) {
        this.compressMediaTypes = compressMediaTypes;
    }

    public String getProduces() {
        return produces;
    }

    public void setProduces(String produces) {
        this.produces = produces;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        ServerHttpResponse response = requestContext.serverResponse();
        String contentEncoding = response.getResponseHeader(HttpHeaders.CONTENT_ENCODING);
        if (contentEncoding != null && io.vertx.core.http.HttpHeaders.IDENTITY.toString().equals(contentEncoding)) {
            switch (compression) {
                case ON:
                    response.removeResponseHeader(HttpHeaders.CONTENT_ENCODING);
                    break;
                case UNDEFINED:
                    EncodedMediaType responseContentType = requestContext.getResponseContentType();
                    if ((responseContentType == null) && (produces != null)) {
                        if (encodedProduces == null) {
                            synchronized (this) {
                                if (encodedProduces == null) {
                                    encodedProduces = new EncodedMediaType(MediaType.valueOf(produces));
                                }
                            }
                        }
                        responseContentType = encodedProduces;
                    }
                    if (responseContentType != null) {
                        MediaType contentType = responseContentType.getMediaType();
                        if (contentType != null
                                && compressMediaTypes.contains(contentType.getType() + '/' + contentType.getSubtype())) {
                            response.removeResponseHeader(HttpHeaders.CONTENT_ENCODING);
                        }
                    }
                    break;
                default:
                    // OFF - no action is needed because the "Content-Encoding: identity" header is set
                    break;
            }
        }
    }
}
