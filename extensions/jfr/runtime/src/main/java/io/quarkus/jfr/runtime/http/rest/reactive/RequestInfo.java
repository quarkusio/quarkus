package io.quarkus.jfr.runtime.http.rest.reactive;

record RequestInfo(String httpMethod, String uri, String remoteAddress) {
}
