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

        for (MediaType mediaType : mediaTypesFromRequest) {
            // It's supported and is included in the `@Produces` annotation
            if (isMediaTypeInList(mediaType, SUPPORTED_MEDIA_TYPES)
                    && isMediaTypeInList(mediaType, mediaTypesFromProducesAnnotation)) {
                return Optional.of(mediaType);
            }
        }

        // if none is found, then return the first from the annotation or empty if no produces annotation
        if (mediaTypesFromProducesAnnotation.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mediaTypesFromProducesAnnotation.get(0));
    }

    private static boolean isMediaTypeInList(MediaType mediaType, List<MediaType> list) {
        for (MediaType item : list) {
            if (mediaType.getType().equalsIgnoreCase(item.getType())
                    && mediaType.getSubtype().equalsIgnoreCase(item.getSubtype())) {
                return true;
            }
        }

        return false;
    }
}
