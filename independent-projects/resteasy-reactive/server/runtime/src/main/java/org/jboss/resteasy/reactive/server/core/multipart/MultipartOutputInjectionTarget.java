package org.jboss.resteasy.reactive.server.core.multipart;

import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataOutput;

public interface MultipartOutputInjectionTarget {
    MultipartFormDataOutput mapFrom(Object pojo);
}
