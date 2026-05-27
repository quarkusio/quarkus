package io.quarkus.resteasy.reactive.server.runtime;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.core.multipart.MultipartMessageBodyWriter;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.reactive.server.multipart.PartItem;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class ResteasyReactiveServerLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = Logger.getLogger("io.quarkus.rest.logging");
    private static final String MASKED_VALUE = "<hidden>";
    private static final String RESPONSE_LOGGED_KEY = "quarkus.rest.logging.response.logged";

    @Inject
    ResteasyReactiveServerRuntimeConfig runtimeConfig;

    @Inject
    CurrentVertxRequest currentVertxRequest;

    @PostConstruct
    void validateConfig() {
        String scope = runtimeConfig.logging().scope();
        if (!"none".equalsIgnoreCase(scope) && !"request-response".equalsIgnoreCase(scope)) {
            log.warnf("Unrecognized value '%s' for 'quarkus.rest.logging.scope'. " +
                    "Valid values are 'none' and 'request-response'. Request-response logging is disabled.", scope);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ResteasyReactiveServerRuntimeConfig.LoggingConfig logging = runtimeConfig.logging();
        if (!"request-response".equalsIgnoreCase(logging.scope())) {
            return;
        }

        Set<String> maskedHeaders = toLowerCase(logging.maskedHeaders());
        String method = sanitize(requestContext.getMethod());
        String uri = sanitize(requestContext.getUriInfo().getRequestUri().toString());
        StringBuilder sb = new StringBuilder("Request: ");
        sb.append(method).append(" ").append(uri);
        sb.append(" ").append(formatHeaders(requestContext.getHeaders(), maskedHeaders));

        if (logging.includeBody()) {
            if (requestContext.hasEntity()) {
                MediaType mediaType = requestContext.getMediaType();
                if (isMultipartMediaType(mediaType)) {
                    String transferEncoding = requestContext.getHeaderString("Transfer-Encoding");
                    String contentLengthHeader = requestContext.getHeaderString("Content-Length");
                    if ("chunked".equalsIgnoreCase(transferEncoding)) {
                        sb.append(", Body: [multipart: chunked]");
                    } else if (contentLengthHeader == null) {
                        sb.append(", Body: [multipart: no content-length]");
                    } else {
                        long contentLength = Long.parseLong(contentLengthHeader);
                        long maxBuffer = logging.bodyBufferLimit().asLongValue();
                        if (contentLength > maxBuffer) {
                            sb.append(", Body: [multipart: ").append(contentLength)
                                    .append(" bytes, body too large to log]");
                        } else {
                            // Content-Length is known and within the buffer limit — safe to buffer fully
                            byte[] bytes = requestContext.getEntityStream().readAllBytes();
                            sb.append(", ").append(formatMultipartBody(bytes, mediaType, logging.bodyLimit()));
                            requestContext.setEntityStream(new ByteArrayInputStream(bytes));
                        }
                    }
                } else if (isTextMediaType(mediaType)) {
                    // Read only bodyLimit+1 bytes so large text bodies are not fully buffered
                    InputStream original = requestContext.getEntityStream();
                    byte[] prefix = original.readNBytes(logging.bodyLimit() + 1);
                    boolean truncated = prefix.length > logging.bodyLimit();
                    String body = sanitize(new String(prefix, StandardCharsets.UTF_8));
                    // sanitize() can expand chars (e.g. \n -> \\n), so cap at bodyLimit chars
                    if (body.length() > logging.bodyLimit()) {
                        body = body.substring(0, logging.bodyLimit());
                    }
                    sb.append(", Body:\n").append(body);
                    if (truncated) {
                        sb.append("...[truncated]");
                    }
                    // Reconstruct the full stream for the endpoint
                    requestContext.setEntityStream(new SequenceInputStream(new ByteArrayInputStream(prefix), original));
                } else {
                    // Binary: log size from Content-Length if present, no buffering needed
                    String contentLength = requestContext.getHeaderString("Content-Length");
                    if (contentLength != null) {
                        sb.append(", Body: [").append(contentLength).append(" bytes]");
                    } else {
                        sb.append(", Body: [binary]");
                    }
                }
            } else {
                sb.append(", Empty body");
            }
        }

        log.info(sb); // lgtm[java/log-injection] - all user-provided values are passed through sanitize()

        // Register a headers-end handler to capture streaming/SSE response starts.
        // For normal (non-SSE) responses, ContainerResponseFilter runs first and sets the RESPONSE_LOGGED_KEY flag,
        // so this handler skips to avoid double-logging.
        // For SSE responses, ContainerResponseFilter is bypassed entirely, so this is the only chance to log.
        RoutingContext routingContext = currentVertxRequest.getCurrent();
        if (routingContext != null) {
            routingContext.addHeadersEndHandler(new Handler<Void>() {
                @Override
                public void handle(Void ignored) {
                    if (Boolean.TRUE.equals(routingContext.get(RESPONSE_LOGGED_KEY))) {
                        return;
                    }
                    Set<String> maskedHdrs = toLowerCase(logging.maskedHeaders());
                    StringBuilder rsb = new StringBuilder("Response: ");
                    rsb.append(method).append(" ").append(uri).append(", ");
                    rsb.append("Status[").append(routingContext.response().getStatusCode())
                            .append(" ").append(sanitize(routingContext.response().getStatusMessage())).append("], ");
                    rsb.append(formatVertxHeaders(routingContext.response().headers(), maskedHdrs));
                    if (logging.includeBody()) {
                        rsb.append(", Body: [streaming]");
                    }
                    log.info(rsb); // lgtm[java/log-injection] - all user-provided values are passed through sanitize()
                }
            });
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        ResteasyReactiveServerRuntimeConfig.LoggingConfig logging = runtimeConfig.logging();
        if (!"request-response".equalsIgnoreCase(logging.scope())) {
            return;
        }

        Set<String> maskedHeaders = toLowerCase(logging.maskedHeaders());
        StringBuilder sb = new StringBuilder("Response: ");
        sb.append(sanitize(requestContext.getMethod())).append(" ")
                .append(sanitize(requestContext.getUriInfo().getRequestUri().toString())).append(", ");
        sb.append("Status[").append(responseContext.getStatus())
                .append(" ").append(sanitize(responseContext.getStatusInfo().getReasonPhrase())).append("], ");
        sb.append(formatHeaders(responseContext.getHeaders(), maskedHeaders));

        if (logging.includeBody()) {
            Object entity = responseContext.getEntity();
            if (entity instanceof MultipartFormDataOutput) {
                sb.append(", ").append(formatMultipartOutput((MultipartFormDataOutput) entity, logging.bodyLimit()));
            } else if (entity instanceof byte[]) {
                sb.append(", Body: [binary ").append(((byte[]) entity).length).append(" bytes]");
            } else if (entity != null) {
                if (isMultipartMediaType(responseContext.getMediaType())) {
                    try {
                        MultipartFormDataOutput formData = MultipartMessageBodyWriter.toFormData(entity);
                        sb.append(", ").append(formatMultipartOutput(formData, logging.bodyLimit()));
                    } catch (RuntimeException e) {
                        // logging should not break request processing
                        sb.append(", Body: [multipart: body not logged]");
                    }
                } else if (isTextMediaType(responseContext.getMediaType())) {
                    sb.append(", Body:\n").append(truncate(sanitize(String.valueOf(entity)), logging.bodyLimit()));
                } else {
                    sb.append(", Body: [binary]");
                }
            }
        }

        log.info(sb); // lgtm[java/log-injection] - all user-provided values are passed through sanitize()

        // Mark response as logged so the headers-end handler registered in the request filter skips
        RoutingContext routingContext = currentVertxRequest.getCurrent();
        if (routingContext != null) {
            routingContext.put(RESPONSE_LOGGED_KEY, Boolean.TRUE);
        }
    }

    private boolean isMultipartMediaType(MediaType mediaType) {
        return mediaType != null && "multipart".equals(mediaType.getType());
    }

    private boolean isTextMediaType(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        return isTextType(mediaType.getType(), mediaType.getSubtype());
    }

    private boolean isTextType(String type, String subtype) {
        if ("text".equals(type)) {
            return true;
        }
        if ("application".equals(type)) {
            return "json".equals(subtype) || subtype.endsWith("+json")
                    || "xml".equals(subtype) || subtype.endsWith("+xml")
                    || "x-www-form-urlencoded".equals(subtype)
                    || "graphql".equals(subtype);
        }
        return false;
    }

    private String formatMultipartOutput(MultipartFormDataOutput output, int limit) {
        StringJoiner parts = new StringJoiner(", ", "Body: Parts[", "]");
        for (Map.Entry<String, List<PartItem>> entry : output.getAllFormData().entrySet()) {
            String name = entry.getKey();
            for (PartItem part : entry.getValue()) {
                StringBuilder partSb = new StringBuilder(sanitize(name));
                String filename = part.getFilename();
                MediaType partMediaType = part.getMediaType();
                if (filename != null) {
                    partSb.append(" filename=").append(sanitize(filename));
                }
                if (partMediaType != null) {
                    partSb.append(" (").append(partMediaType.getType()).append("/").append(partMediaType.getSubtype())
                            .append(")");
                }
                partSb.append(": ");

                Object entity = part.getEntity();
                if (entity instanceof byte[]) {
                    partSb.append("[").append(((byte[]) entity).length).append(" bytes]");
                } else if (entity instanceof File) {
                    File file = (File) entity;
                    partSb.append("[file: ").append(sanitize(file.getName())).append(", ").append(file.length())
                            .append(" bytes]");
                } else if (entity instanceof Path) {
                    Path path = (Path) entity;
                    partSb.append("[file: ").append(sanitize(path.getFileName().toString())).append(", ")
                            .append(path.toFile().length()).append(" bytes]");
                } else if (entity != null) {
                    // null media type defaults to text/plain
                    if (partMediaType == null || isTextMediaType(partMediaType)) {
                        partSb.append(truncate(sanitize(String.valueOf(entity)), limit));
                    } else {
                        partSb.append("[binary object]");
                    }
                }
                parts.add(partSb.toString());
            }
        }
        return parts.toString();
    }

    private String formatMultipartBody(byte[] rawBytes, MediaType contentType, int limit) {
        String boundary = contentType.getParameters().get("boundary");
        if (boundary == null) {
            return "Body: [multipart: boundary not found]";
        }

        // ISO-8859-1 gives a 1:1 byte<->char mapping so we can locate structure without
        // corrupting binary part payloads when we later re-extract them as raw bytes.
        String raw = new String(rawBytes, StandardCharsets.ISO_8859_1);
        String delimiter = "--" + boundary;

        StringJoiner parts = new StringJoiner(", ", "Body: Parts[", "]");
        int pos = 0;

        while (true) {
            int delimStart = raw.indexOf(delimiter, pos);
            if (delimStart < 0) {
                break;
            }
            int afterDelim = delimStart + delimiter.length();

            // "--boundary--" is the closing marker
            if (afterDelim + 1 < raw.length()
                    && raw.charAt(afterDelim) == '-' && raw.charAt(afterDelim + 1) == '-') {
                break;
            }

            // Skip the CRLF that follows the delimiter line
            int partStart = afterDelim;
            if (partStart < raw.length() && raw.charAt(partStart) == '\r') {
                partStart++;
            }
            if (partStart < raw.length() && raw.charAt(partStart) == '\n') {
                partStart++;
            }

            // The next delimiter (preceded by CRLF) marks the end of this part's body
            int partEnd = raw.indexOf("\r\n" + delimiter, partStart);
            if (partEnd < 0) {
                break;
            }

            String part = raw.substring(partStart, partEnd);
            int headerEnd = part.indexOf("\r\n\r\n");
            String partHeaders = headerEnd >= 0 ? part.substring(0, headerEnd) : "";
            String partBodyIso = headerEnd >= 0 ? part.substring(headerEnd + 4) : part;
            // Re-obtain the original bytes of the part body via ISO-8859-1 round-trip
            byte[] partBodyBytes = partBodyIso.getBytes(StandardCharsets.ISO_8859_1);

            String name = extractDispositionParam(partHeaders, "name");
            String filename = extractDispositionParam(partHeaders, "filename");
            MediaType partMediaType = extractPartMediaType(partHeaders);

            StringBuilder partSb = new StringBuilder(name != null ? sanitize(name) : "?");
            if (filename != null) {
                partSb.append(" filename=").append(sanitize(filename));
            }
            if (partMediaType != null) {
                partSb.append(" (").append(partMediaType.getType()).append("/").append(partMediaType.getSubtype())
                        .append(")");
            }
            partSb.append(": ");

            // null media type defaults to text/plain
            if (partMediaType == null || isTextMediaType(partMediaType)) {
                partSb.append(truncate(sanitize(new String(partBodyBytes, StandardCharsets.UTF_8)), limit));
            } else {
                partSb.append("[").append(partBodyBytes.length).append(" bytes]");
            }

            parts.add(partSb.toString());
            pos = partEnd + 2; // advance past the \r\n before the next delimiter
        }

        return parts.toString();
    }

    private String extractDispositionParam(String headers, String param) {
        String headersLower = headers.toLowerCase(Locale.ROOT);
        int cdIdx = headersLower.indexOf("content-disposition:");
        if (cdIdx < 0) {
            return null;
        }
        int lineEnd = headers.indexOf("\r\n", cdIdx);
        String cdLine = lineEnd >= 0 ? headers.substring(cdIdx, lineEnd) : headers.substring(cdIdx);

        String searchFor = param.toLowerCase(Locale.ROOT) + "=";
        int paramIdx = cdLine.toLowerCase(Locale.ROOT).indexOf(searchFor);
        if (paramIdx < 0) {
            return null;
        }
        int valueStart = paramIdx + searchFor.length();
        if (valueStart >= cdLine.length()) {
            return null;
        }
        if (cdLine.charAt(valueStart) == '"') {
            int endQuote = cdLine.indexOf('"', valueStart + 1);
            return endQuote >= 0 ? cdLine.substring(valueStart + 1, endQuote) : cdLine.substring(valueStart + 1);
        }
        int end = cdLine.indexOf(';', valueStart);
        return (end >= 0 ? cdLine.substring(valueStart, end) : cdLine.substring(valueStart)).trim();
    }

    private MediaType extractPartMediaType(String headers) {
        String headersLower = headers.toLowerCase(Locale.ROOT);
        int ctIdx = headersLower.indexOf("content-type:");
        if (ctIdx < 0) {
            return null;
        }
        int lineEnd = headers.indexOf("\r\n", ctIdx);
        String ct = lineEnd >= 0 ? headers.substring(ctIdx + 13, lineEnd) : headers.substring(ctIdx + 13);
        try {
            return MediaType.valueOf(ct.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String formatHeaders(MultivaluedMap<String, ?> headers, Set<String> maskedHeaders) {
        StringJoiner joiner = new StringJoiner(" ", "Headers[", "]");
        for (Map.Entry<String, ? extends List<?>> entry : headers.entrySet()) {
            joiner.add(entry.getKey() + "=" + maskHeaderValue(entry.getKey(), entry.getValue(), maskedHeaders));
        }
        return joiner.toString();
    }

    private String formatVertxHeaders(Iterable<Map.Entry<String, String>> headers, Set<String> maskedHeaders) {
        StringJoiner joiner = new StringJoiner(" ", "Headers[", "]");
        for (Map.Entry<String, String> header : headers) {
            joiner.add(header.getKey() + "=" + maskSingleHeaderValue(header.getKey(), header.getValue(), maskedHeaders));
        }
        return joiner.toString();
    }

    private String truncate(String s, int limit) {
        if (s.length() <= limit) {
            return s;
        }
        return s.substring(0, limit) + "...[truncated]";
    }

    private String sanitize(String value) {
        return value
                .replace("\r\n", "\\r\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("${", "$\\{");
    }

    private String maskHeaderValue(String name, List<?> values, Set<String> maskedHeaders) {
        if (maskedHeaders.contains(name.toLowerCase(Locale.ROOT))) {
            return MASKED_VALUE;
        }
        if (values.size() == 1) {
            return sanitize(values.get(0).toString());
        }
        return values.stream().map(v -> sanitize(v.toString())).collect(Collectors.joining(","));
    }

    private String maskSingleHeaderValue(String name, String value, Set<String> maskedHeaders) {
        if (maskedHeaders.contains(name.toLowerCase(Locale.ROOT))) {
            return MASKED_VALUE;
        }
        return sanitize(value);
    }

    private Set<String> toLowerCase(Set<String> headers) {
        return headers.stream().map(h -> h.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }
}
