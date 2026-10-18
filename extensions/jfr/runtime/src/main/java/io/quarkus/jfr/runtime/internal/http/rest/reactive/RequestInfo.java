package io.quarkus.jfr.runtime.internal.http.rest.reactive;

record RequestInfo(String httpMethod, String uri, String remoteAddress) {
}
