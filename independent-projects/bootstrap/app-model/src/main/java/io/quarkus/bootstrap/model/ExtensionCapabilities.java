package io.quarkus.bootstrap.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;

public interface ExtensionCapabilities extends Mappable {

    static ExtensionCapabilities fromMap(Map<String, Object> map) {
        return new CapabilityContract(
                map.get(BootstrapConstants.MAPPABLE_EXTENSION).toString(),
                (Collection<String>) map.getOrDefault(BootstrapConstants.MAPPABLE_PROVIDED, List.of()),
                (Collection<String>) map.getOrDefault(BootstrapConstants.MAPPABLE_REQUIRED, List.of()));
    }

    String getExtension();

    Collection<String> getProvidesCapabilities();

    Collection<String> getRequiresCapabilities();

    @Override
    default Map<String, Object> asMap(MappableCollectionFactory factory) {
        final Map<String, Object> map = factory.newMap(3);
        map.put(BootstrapConstants.MAPPABLE_EXTENSION, getExtension());
        if (!getProvidesCapabilities().isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_PROVIDED, Mappable.toStringCollection(getProvidesCapabilities(), factory));
        }
        if (!getRequiresCapabilities().isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_REQUIRED, Mappable.toStringCollection(getRequiresCapabilities(), factory));
        }
        return map;
    }
}
