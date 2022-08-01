package io.quarkus.arc.processor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

final class AssignabilityCheck {

    private final ConcurrentMap<DotName, Set<DotName>> cache;
    private final IndexView index;

    public AssignabilityCheck(IndexView beanArchiveIndex, IndexView applicationIndex) {
        this.cache = new ConcurrentHashMap<>();
        this.index = applicationIndex != null ? CompositeIndex.create(beanArchiveIndex, applicationIndex) : beanArchiveIndex;
    }

    boolean isAssignableFrom(Type type1, Type type2) {
        // java.lang.Object is assignable from any type
        if (type1.name().equals(DotNames.OBJECT)) {
            return true;
        }
        // type1 is the same as type2
        if (type1.name().equals(type2.name())) {
            return true;
        }
        // type1 is a superclass
        return getAssignables(type1.name()).contains(type2.name());
    }

    Set<DotName> getAssignables(DotName name) {
        return cache.computeIfAbsent(name, this::findAssignables);
    }

    private Set<DotName> findAssignables(DotName name) {
        Set<DotName> assignables = new HashSet<>();
        for (ClassInfo subclass : index.getAllKnownSubclasses(name)) {
            assignables.add(subclass.name());
        }
        for (ClassInfo implementor : index.getAllKnownImplementors(name)) {
            assignables.add(implementor.name());
        }
        return assignables;
    }

}
