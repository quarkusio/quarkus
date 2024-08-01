package io.quarkus.deployment;

import java.util.*;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class CollectionClassProcessor {
    @BuildStep
    ReflectiveClassBuildItem setupCollectionClasses() {
        return ReflectiveClassBuildItem.builder(ArrayList.class,
                HashMap.class,
                HashSet.class,
                LinkedList.class,
                LinkedHashMap.class,
                LinkedHashSet.class,
                TreeMap.class,
                TreeSet.class).reason(getClass().getName()).build();
    }
}
