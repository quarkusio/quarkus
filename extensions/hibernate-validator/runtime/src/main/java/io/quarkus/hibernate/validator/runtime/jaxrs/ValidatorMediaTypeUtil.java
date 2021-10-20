package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.MediaType;

/**
 * Utility class to deal with MediaTypes in JAX-RS endpoints.
 */
public final class ValidatorMediaTypeUtil {

    private static final List<MediaType> SUPPORTED_MEDIA_TYPES = Arrays.asList(MediaType.APPLICATION_XML_TYPE,
            MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE);

    private ValidatorMediaTypeUtil() {

    }

    /**
     * Look up the right media type taking into account the HTTP request and the media types defined in the `@Produces`
     * annotation.
     *
     * @param mediaTypesFromRequest list of media types in the HTTP request.
     * @param mediaTypesFromProducesAnnotation list of media types set in the `@Produces` annotation.
     * @return one supported media type from either the HTTP request or the annotation.
     */
    public static Optional<MediaType> getAcceptMediaType(List<MediaType> mediaTypesFromRequest,
            List<MediaType> mediaTypesFromProducesAnnotation) {

        Optional<MediaType> mediaType = mediaTypesFromRequest.stream()
                // It's supported
                .filter(SUPPORTED_MEDIA_TYPES::contains)
                // It's included in the `@Produces` annotation
                .filter(mediaTypesFromProducesAnnotation::contains)
                .findFirst()
                // if none is found, then return the first from the annotation
                .or(mediaTypesFromProducesAnnotation.stream()::findFirst);

        return mediaType;
    }
}
