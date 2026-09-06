package io.quarkus.resteasy.reactive.server.runtime.produi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.util.RuntimeResourceVisitor;

import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;

/**
 * Read-only Prod UI view of the registered JAX-RS (Quarkus REST) endpoints. For
 * each endpoint it exposes the HTTP method, full path, path parameters, resource
 * class and the produced / consumed media types, derived from the always-present
 * runtime {@link Deployment}. It deliberately omits the Dev UI's endpoint-score
 * diagnostics (a dev-only scoring tool) and does not offer any request
 * invocation - it is purely a listing of the endpoint metadata.
 */
@ApplicationScoped
public class ResteasyReactiveProdUIService {

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only list of the registered JAX-RS endpoints with their media types and parameters")
    public List<EndpointInfo> getEndpoints() {
        Deployment deployment = ResteasyReactiveRecorder.getCurrentDeployment();
        if (deployment == null) {
            return List.of();
        }

        List<EndpointInfo> endpoints = new ArrayList<>();
        RuntimeResourceVisitor.visitRuntimeResources(deployment.getPrefix(), deployment.getClassMappers(),
                new RuntimeResourceVisitor() {
                    @Override
                    public void visitRuntimeResource(String httpMethod, String fullPath, RuntimeResource runtimeResource) {
                        endpoints.add(new EndpointInfo(httpMethod, fullPath,
                                new ArrayList<>(new TreeSet<>(runtimeResource.getPathParameterIndexes().keySet())),
                                runtimeResource.getResourceClass().getName(),
                                producesOf(runtimeResource),
                                mediaTypesToString(runtimeResource.getConsumes())));
                    }
                });

        endpoints.sort(Comparator.comparing(EndpointInfo::path).thenComparing(EndpointInfo::httpMethod));
        return endpoints;
    }

    private List<String> producesOf(RuntimeResource runtimeResource) {
        ServerMediaType serverMediaType = runtimeResource.getProduces();
        if (serverMediaType != null && serverMediaType.getSortedOriginalMediaTypes() != null
                && serverMediaType.getSortedOriginalMediaTypes().length >= 1) {
            return mediaTypesToString(Arrays.asList(serverMediaType.getSortedOriginalMediaTypes()));
        }
        return List.of();
    }

    private List<String> mediaTypesToString(List<MediaType> mediaTypes) {
        if (mediaTypes == null) {
            return List.of();
        }
        return mediaTypes.stream().map(MediaType::toString).collect(Collectors.toList());
    }

    public record EndpointInfo(String httpMethod, String path, List<String> pathParameters, String resourceClass,
            List<String> produces, List<String> consumes) {
    }
}
