package org.jboss.resteasy.reactive.server.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.headers.MediaTypeHeaderDelegate;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;

class HandlerMediaTypeUtil {

    private static final String INVALID_ACCEPT_HEADER_MESSAGE = "The accept header value did not match the value in @Produces";

    // according to the spec we need to return HTTP 415 when content-type header doesn't match what is specified in @Consumes
    // HttpMethod being null means this is a sub resource locator method. The handler chain of the sub resource has to match the content-type header
    static void validateConsumes(RequestMapper.RequestMatch<RuntimeResource> target,
            ResteasyReactiveRequestContext requestContext) {
        if (target.value.getHttpMethod() != null && !target.value.getConsumes().isEmpty()) {
            String contentType = (String) requestContext.getHeader(HttpHeaders.CONTENT_TYPE, true);
            if (contentType != null) {
                try {
                    if (MediaTypeHelper.getFirstMatch(
                            target.value.getConsumes(),
                            Collections.singletonList(MediaTypeHelper.valueOf(contentType))) == null) {
                        throw new NotSupportedException("The content-type header value did not match the value in @Consumes");
                    }
                } catch (IllegalArgumentException e) {
                    throw new NotSupportedException("The content-type header value did not correspond to a valid media type");
                }
            }
        }
    }

    // according to the spec we need to return HTTP 406 when Accept header doesn't match what is specified in @Produces
    // HttpMethod being null means this is a sub resource locator method. The handler chain of the sub resource has to match the accept header
    static void validateProduces(RequestMapper.RequestMatch<RuntimeResource> target,
            ResteasyReactiveRequestContext requestContext) {
        if (target.value.getHttpMethod() != null && target.value.getProduces() != null) {
            // there could potentially be multiple Accept headers and we need to response with 406
            // if none match the method's @Produces
            List<String> accepts = (List<String>) requestContext.getHeader(HttpHeaders.ACCEPT, false);
            if (!accepts.isEmpty()) {
                boolean hasAtLeastOneMatch = false;
                for (int i = 0; i < accepts.size(); i++) {
                    try {
                        boolean matches = acceptHeaderMatches(target, accepts.get(i));
                        if (matches) {
                            hasAtLeastOneMatch = true;
                            break;
                        }
                    } catch (IllegalArgumentException ignored) {
                        // the provided header was not valid
                    }
                }
                if (!hasAtLeastOneMatch) {
                    throw new NotAcceptableException(INVALID_ACCEPT_HEADER_MESSAGE);
                }
            }

            requestContext.setProducesChecked(true);
        }
    }

    /**
     * @return {@code true} if the provided string matches one of the {@code @Produces} values of the resource method
     * @throws IllegalArgumentException if the provided string cannot be parsed into a {@link MediaType}
     */
    private static boolean acceptHeaderMatches(RequestMapper.RequestMatch<RuntimeResource> target, String accepts) {
        if ((accepts != null) && !accepts.equals(MediaType.WILDCARD)) {
            int commaIndex = accepts.indexOf(',');
            boolean multipleAcceptsValues = commaIndex >= 0;
            MediaType[] producesMediaTypes = target.value.getProduces().getSortedOriginalMediaTypes();
            if (!multipleAcceptsValues && (producesMediaTypes.length == 1)) {
                // the point of this branch is to eliminate any list creation or string indexing as none is needed
                MediaType providedMediaType = producesMediaTypes[0];
                return providedMediaType.isCompatible(toMediaType(accepts.trim()));
            } else if (multipleAcceptsValues && (producesMediaTypes.length == 1)) {
                // this is fairly common case, so we want it to be as fast as possible
                // we do that by manually splitting the accepts header and immediately checking
                // if the value is compatible with the produces media type
                boolean compatible = false;
                int begin = 0;

                do {
                    String acceptPart;
                    if (commaIndex == -1) { // this is the case where we are checking the remainder of the string
                        acceptPart = accepts.substring(begin);
                    } else {
                        acceptPart = accepts.substring(begin, commaIndex);
                    }
                    if (producesMediaTypes[0].isCompatible(toMediaType(acceptPart.trim()))) {
                        compatible = true;
                        break;
                    } else if (commaIndex == -1) { // we have reached the end and not found any compatible media types
                        break;
                    }
                    begin = commaIndex + 1; // the next part will start at the character after the comma
                    if (begin >= (accepts.length() - 1)) { // if we have reached this point, then are no compatible media types
                        break;
                    }
                    commaIndex = accepts.indexOf(',', begin);
                } while (true);

                return compatible;
            } else {
                // don't use any of the JAX-RS stuff from the various MediaType helper as we want to be as performant as possible
                List<MediaType> acceptsMediaTypes;
                if (accepts.contains(",")) {
                    String[] parts = accepts.split(",");
                    acceptsMediaTypes = new ArrayList<>(parts.length);
                    for (int i = 0; i < parts.length; i++) {
                        String part = parts[i];
                        acceptsMediaTypes.add(toMediaType(part.trim()));
                    }
                } else {
                    acceptsMediaTypes = Collections.singletonList(toMediaType(accepts));
                }
                return MediaTypeHelper.getFirstMatch(Arrays.asList(producesMediaTypes),
                        acceptsMediaTypes) != null;
            }
        }

        return true;
    }

    private static MediaType toMediaType(String mediaTypeStr) {
        return MediaTypeHeaderDelegate.parse(mediaTypeStr);
    }
}
