package io.quarkus.resteasy.reactive.links.runtime;

import java.util.Set;

public record LinkInfo(String rel, String title, String type, String entityType, String path, Set<String> pathParameters) {
}
