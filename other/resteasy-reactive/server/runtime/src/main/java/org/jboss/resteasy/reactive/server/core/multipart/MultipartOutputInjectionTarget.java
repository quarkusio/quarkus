package org.jboss.resteasy.reactive.server.core.multipart;

import java.util.List;

public interface MultipartOutputInjectionTarget {
    List<PartItem> mapFrom(Object pojo);
}
