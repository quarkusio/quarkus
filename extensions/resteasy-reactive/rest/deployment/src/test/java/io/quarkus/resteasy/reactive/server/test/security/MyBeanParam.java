package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.ws.rs.BeanParam;

import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestQuery;

public record MyBeanParam(@RestQuery String queryParam, @BeanParam Headers headers) {
    public record Headers(@RestHeader String authorization) {
    }
}
