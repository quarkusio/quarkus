package io.quarkus.deployment;

import java.util.*;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

public class CollectionClassProcessor {
    @BuildStep
    ReflectiveClassBuildItem setupCollectionClasses() {
        return new ReflectiveClassBuildItem(false, false,
                ArrayList.class.getName(),
                HashMap.class.getName(),
                HashSet.class.getName(),
                LinkedList.class.getName(),
                LinkedHashMap.class.getName(),
                LinkedHashSet.class.getName(),
                TreeMap.class.getName(),
                TreeSet.class.getName());
    }
}
