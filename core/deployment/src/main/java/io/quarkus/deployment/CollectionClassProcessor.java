package io.quarkus.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

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
