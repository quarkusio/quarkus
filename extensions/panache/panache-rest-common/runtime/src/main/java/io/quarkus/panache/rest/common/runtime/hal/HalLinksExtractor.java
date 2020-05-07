package io.quarkus.panache.rest.common.runtime.hal;

import java.util.Map;

public interface HalLinksExtractor {

    Map<String, HalLink> getLinks(Class<?> entityClass);

    Map<String, HalLink> getLinks(Object entity);
}
