package io.quarkus.rest.client.reactive.runtime;

import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.common.util.Encode;

@SuppressWarnings("unused")
public final class ClientQueryParamSupport {

    private ClientQueryParamSupport() {
    }

    public static boolean isQueryParamPresent(WebTargetImpl webTarget, String name) {
        String query = webTarget.getUriBuilderUnsafe().getQuery();
        if (query == null || query.isEmpty()) {
            return false;
        }
        // Must match the encoding WebTargetImpl.queryParam applies to names, via UriBuilderImpl.clientQueryParam.
        String encodedName = Encode.encodeQueryParamAsIs(name);
        int nameLength = encodedName.length();
        int start = 0;
        while (start <= query.length()) {
            int end = query.indexOf('&', start);
            if (end < 0) {
                end = query.length();
            }
            if (query.startsWith(encodedName, start)
                    && (end - start == nameLength || query.charAt(start + nameLength) == '=')) {
                return true;
            }
            start = end + 1;
        }
        return false;
    }
}
