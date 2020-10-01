package io.quarkus.rest.runtime.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.vertx.core.http.HttpServerRequest;

/**
 * A representation of a server side media type.
 */
public class ServerMediaType {

    public static final ServerMediaType EMPTY = new ServerMediaType(new String[0], StandardCharsets.UTF_8.name());

    private final MediaType[] sortedMediaTypes;
    private final MediaType[] sortedOriginalMediaTypes;
    private final MediaType hardCoded;

    public ServerMediaType(String[] mediaTypes, String charset) {
        this(Arrays.stream(mediaTypes).map(MediaType::valueOf).collect(Collectors.toList()), charset);
    }

    public ServerMediaType(List<MediaType> mediaTypes, String charset) {
        if (mediaTypes.isEmpty()) {
            this.sortedOriginalMediaTypes = new MediaType[] { MediaType.WILDCARD_TYPE };
        } else {
            this.sortedOriginalMediaTypes = mediaTypes.toArray(new MediaType[0]);
        }
        sortedMediaTypes = new MediaType[sortedOriginalMediaTypes.length];
        Arrays.sort(sortedOriginalMediaTypes, new Comparator<MediaType>() {
            @Override
            public int compare(MediaType m1, MediaType m2) {
                String qs1s = m1.getParameters().get("qs");
                String qs2s = m2.getParameters().get("qs");
                if (qs1s == null && qs2s == null) {
                    return 0;
                }
                if (qs1s != null) {
                    if (qs2s == null) {
                        return 1;
                    } else {
                        float q1 = Float.parseFloat(qs1s);
                        float q2 = Float.parseFloat(qs2s);
                        return Float.compare(q2, q1);
                    }
                } else {
                    return -1;
                }
            }
        });
        for (int i = 0; i < sortedOriginalMediaTypes.length; ++i) {
            MediaType existing = sortedOriginalMediaTypes[i];
            MediaType m = new MediaType(existing.getType(), existing.getSubtype(), charset);
            sortedMediaTypes[i] = m;
        }
        if (sortedMediaTypes.length == 1
                && !(sortedMediaTypes[0].isWildcardType() || sortedMediaTypes[0].isWildcardSubtype())) {
            hardCoded = sortedMediaTypes[0];
        } else {
            hardCoded = null;
        }
    }

    public MediaType negotiateProduces(HttpServerRequest request) {
        return negotiateProduces(request, this.hardCoded, NegotiateFallbackStrategy.SERVER);
    }

    public MediaType negotiateProduces(HttpServerRequest request, MediaType hardCoded,
            NegotiateFallbackStrategy negotiateFallbackStrategy) {
        if (hardCoded != null) {
            //technically we should negotiate here, and check if we need to return a 416
            //but for performance reasons we ignore this
            return hardCoded;
        }
        String acceptStr = request.getHeader(HttpHeaders.ACCEPT);
        MediaType selected = null;
        List<MediaType> parsedAccepted = Collections.emptyList();
        if (acceptStr != null) {
            //TODO: this can be optimised
            parsedAccepted = MediaTypeHelper.parseHeader(acceptStr);
            MediaTypeHelper.sortByWeight(parsedAccepted);
            String currentClientQ = null;
            int currentServerIndex = Integer.MAX_VALUE;
            if (!parsedAccepted.isEmpty()) {
                for (MediaType desired : parsedAccepted) {
                    if (selected != null) {
                        //this is to enable server side q values to take effect
                        //the client side is sorted by q, if we have already picked one and the q is
                        //different then we can return the current one
                        if (!Objects.equals(desired.getParameters().get("q"), currentClientQ)) {
                            if (selected.equals(MediaType.WILDCARD_TYPE)) {
                                return MediaType.APPLICATION_OCTET_STREAM_TYPE;
                            }
                            return selected;
                        }
                    }
                    for (int j = 0; j < sortedMediaTypes.length; j++) {
                        MediaType provide = sortedMediaTypes[j];
                        if (provide.isCompatible(desired)) {
                            if (selected == null || j < currentServerIndex) {
                                if (desired.isWildcardType()) {
                                    selected = MediaType.APPLICATION_OCTET_STREAM_TYPE;
                                } else {
                                    selected = desired;
                                }
                                currentServerIndex = j;
                                currentClientQ = desired.getParameters().get("q");
                            }
                        }
                    }
                }
            }
        }
        if (selected == null) {
            if ((negotiateFallbackStrategy == NegotiateFallbackStrategy.CLIENT) && (!parsedAccepted.isEmpty())) {
                selected = parsedAccepted.get(0);
            } else {
                selected = sortedMediaTypes[0];
            }

        }
        if (selected.equals(MediaType.WILDCARD_TYPE)) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        return selected;
    }

    public MediaType[] getSortedMediaTypes() {
        return sortedMediaTypes;
    }

    public MediaType[] getSortedOriginalMediaTypes() {
        return sortedOriginalMediaTypes;
    }

    public enum NegotiateFallbackStrategy {
        SERVER,
        CLIENT
    }
}
