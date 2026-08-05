package org.jboss.resteasy.reactive.client.impl;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.client.BasicRestResponse;

record BasicRestResponseImpl(int status, MultivaluedMap<String, String> headers) implements BasicRestResponse {

}
