package io.quarkus.resteasy.reactive.server.runtime.multipart;

import java.util.List;

public interface MultipartOutputInjectionTarget {
    List<PartItem> mapFrom(Object pojo);
}
