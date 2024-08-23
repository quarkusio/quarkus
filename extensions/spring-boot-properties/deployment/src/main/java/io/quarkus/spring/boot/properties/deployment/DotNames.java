package io.quarkus.spring.boot.properties.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.DotName;

final class DotNames {

    private DotNames() {
    }

    static final DotName OBJECT = DotName.createSimple(Object.class.getName());
    static final DotName STRING = DotName.createSimple(String.class.getName());
    static final DotName OPTIONAL = DotName.createSimple(Optional.class.getName());
    static final DotName LIST = DotName.createSimple(List.class.getName());
    static final DotName SET = DotName.createSimple(Set.class.getName());
    static final DotName COLLECTION = DotName.createSimple(Collection.class.getName());
    static final DotName MAP = DotName.createSimple(Map.class.getName());
    static final DotName HASH_MAP = DotName.createSimple(HashMap.class.getName());
    static final DotName ENUM = DotName.createSimple(Enum.class.getName());
    static final DotName MP_CONFIG_PROPERTIES = DotName
            .createSimple(org.eclipse.microprofile.config.inject.ConfigProperties.class.getName());
    static final DotName CONFIG_PROPERTY = DotName.createSimple(ConfigProperty.class.getName());
}
