package org.jboss.resteasy.reactive.server.core.multipart;

public interface MultipartOutputInjectionTarget {
    MultipartFormDataOutput mapFrom(Object pojo);
}
