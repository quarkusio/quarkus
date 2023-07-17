package io.quarkus.spring.web.runtime.common;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Variant;

public abstract class AbstractResponseContentTypeResolver {

    private static final MediaType DEFAULT_MEDIA_TYPE = TEXT_PLAIN_TYPE;

    protected abstract Variant negotiateBestMatch(List<String> acceptHeaders, List<Variant> variants);

    public MediaType resolve(HttpHeaders httpHeaders, String... supportedMediaTypes) {
        Objects.requireNonNull(httpHeaders, "HttpHeaders cannot be null");
        Objects.requireNonNull(supportedMediaTypes, "Supported media types array cannot be null");

        Variant bestVariant = getBestVariant(httpHeaders.getRequestHeader(ACCEPT), getMediaTypeVariants(supportedMediaTypes));

        if (bestVariant != null) {
            return bestVariant.getMediaType();
        }

        if (supportedMediaTypes.length > 0) {
            return MediaType.valueOf(supportedMediaTypes[0]);
        }

        return DEFAULT_MEDIA_TYPE;
    }

    private Variant getBestVariant(List<String> acceptHeaders, List<Variant> variants) {
        if (acceptHeaders.isEmpty()) {
            // done because negotiation.setAcceptHeaders(acceptHeaders) throws a NPE when passed an empty list
            return null;
        }

        return negotiateBestMatch(acceptHeaders, variants);
    }

    private static List<Variant> getMediaTypeVariants(String... mediaTypes) {
        Variant.VariantListBuilder variantListBuilder = Variant.VariantListBuilder.newInstance();

        for (String mediaType : mediaTypes) {
            variantListBuilder.mediaTypes(MediaType.valueOf(mediaType));
        }

        return variantListBuilder.build();
    }
}
