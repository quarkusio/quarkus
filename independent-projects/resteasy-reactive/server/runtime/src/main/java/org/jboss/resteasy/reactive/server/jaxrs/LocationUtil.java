package org.jboss.resteasy.reactive.server.jaxrs;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ForwardedInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;

final class LocationUtil {

    static URI determineLocation(URI location) {
        if (!location.isAbsolute()) {
            // FIXME: this leaks server stuff onto the client
            ResteasyReactiveRequestContext request = CurrentRequestManager.get();
            if (request != null) {
                location = getUri(location.toString(), request, true);
            }
        }
        return location;
    }

    static URI getUri(String path, ResteasyReactiveRequestContext request, boolean usePrefix) {
        try {
            String prefix = usePrefix ? determinePrefix(request.serverRequest(), request.getDeployment()) : "";
            // Spec says relative to request, but TCK tests relative to Base URI, so we do that
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            URI uri = new URI(request.getScheme(), request.getAuthority(), "/", null, null);
            if (prefix.isEmpty() && path.equals("/")) {
                return uri;
            }
            return uri.resolve(prefix + path);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String determinePrefix(ServerHttpRequest serverHttpRequest, Deployment deployment) {
        String prefix = "";
        if (deployment != null) {
            // prefix is already sanitised
            prefix = deployment.getPrefix();
        }
        ForwardedInfo forwardedInfo = serverHttpRequest.getForwardedInfo();
        if (forwardedInfo != null) {
            if ((forwardedInfo.getPrefix() != null) && !forwardedInfo.getPrefix().isEmpty()) {
                String forwardedPrefix = forwardedInfo.getPrefix();
                if (!forwardedPrefix.startsWith("/")) {
                    forwardedPrefix = "/" + forwardedPrefix;
                }
                prefix = forwardedPrefix + prefix;
            }
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    static URI determineContentLocation(URI location) {
        if (!location.isAbsolute()) {
            ResteasyReactiveRequestContext request = CurrentRequestManager.get();
            if (request != null) {
                // FIXME: this leaks server stuff onto the client
                location = getUri(location.toString(), request, false);
            }
        }
        return location;
    }
}
