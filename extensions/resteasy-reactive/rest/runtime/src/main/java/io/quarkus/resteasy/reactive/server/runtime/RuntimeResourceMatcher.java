package io.quarkus.resteasy.reactive.server.runtime;

import java.util.Map;

import jakarta.ws.rs.HttpMethod;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.handlers.ClassRoutingHandler;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper.RequestMatch;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Finds the resource method a request is dispatched to the same way {@link RestInitialHandler} and
 * {@link ClassRoutingHandler} do, without invoking it.
 */
public final class RuntimeResourceMatcher {

    private final RequestMapper<RestInitialHandler.InitialMatch> classMapper;

    public RuntimeResourceMatcher(Deployment deployment) {
        this.classMapper = new RequestMapper<>(deployment.getClassMappers());
    }

    /**
     * @param path the request path without the prefix of the deployment, see
     *        {@code ResteasyReactiveRequestContext#getPathWithoutPrefix()}
     * @return the match, or {@code null} if no resource method matches
     */
    public Match match(String path, String httpMethod) {
        // like ClassRoutingHandler with restartWithNextInitialMatch(), the next matching class is tried when no
        // method of the best matching class matches
        RequestMatch<RestInitialHandler.InitialMatch> classMatch = classMapper.map(path);
        while (classMatch != null) {
            RequestMatch<RuntimeResource> methodMatch = matchMethod(classMatch, httpMethod);
            if (methodMatch != null) {
                return new Match(classMatch, methodMatch);
            }
            classMatch = classMapper.continueMatching(path, classMatch);
        }
        return null;
    }

    private static RequestMatch<RuntimeResource> matchMethod(RequestMatch<RestInitialHandler.InitialMatch> classMatch,
            String httpMethod) {
        ServerRestHandler[] handlers = classMatch.value.handlers;
        if (handlers == null || handlers.length == 0 || !(handlers[0] instanceof ClassRoutingHandler classRoutingHandler)) {
            return null;
        }
        Map<String, RequestMapper<RuntimeResource>> mappers = classRoutingHandler.getMappers();
        String remaining = classMatch.remaining.isEmpty() ? "/" : classMatch.remaining;
        // like ClassRoutingHandler, HEAD and OPTIONS requests fall back to the GET methods, then to the sub-resource
        // locators
        RequestMapper<RuntimeResource> mapper = mappers.get(httpMethod);
        if (mapper == null) {
            if (httpMethod.equals(HttpMethod.HEAD) || httpMethod.equals(HttpMethod.OPTIONS)) {
                mapper = mappers.get(HttpMethod.GET);
            }
            if (mapper == null) {
                mapper = mappers.get(null);
            }
            if (mapper == null) {
                return null;
            }
        }
        RequestMatch<RuntimeResource> match = mapper.map(remaining);
        if (match == null && httpMethod.equals(HttpMethod.HEAD) && mappers.get(HttpMethod.GET) != null) {
            match = mappers.get(HttpMethod.GET).map(remaining);
        }
        return match;
    }

    public record Match(RequestMatch<RestInitialHandler.InitialMatch> classMatch,
            RequestMatch<RuntimeResource> methodMatch) {

        public RuntimeResource resource() {
            return methodMatch.value;
        }

        /**
         * Returns the path template of the class and the method, without a trailing slash.
         */
        public String template() {
            String template = classMatch.template.template + methodMatch.template.template;
            return template.endsWith("/") ? template.substring(0, template.length() - 1) : template;
        }
    }
}
