package io.quarkus.arc.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationInstanceEquivalenceProxy;
import org.jboss.jandex.DotName;
import org.jboss.jandex.EquivalenceKey;
import org.jboss.jandex.Type;

final class Unique {
    static Set<Type> types(Set<Type> types) {
        return types(types, HashSet::new);
    }

    static List<Type> types(List<Type> types) {
        return types(types, ArrayList::new);
    }

    private static <C extends Collection<Type>> C types(C types, Supplier<C> creator) {
        if (types.size() <= 1) {
            return types;
        }

        C result = creator.get();
        Set<EquivalenceKey> seen = new HashSet<>((int) (types.size() / 0.75f) + 1);
        for (Type type : types) {
            if (seen.add(EquivalenceKey.of(type))) {
                result.add(type);
            }
        }
        return result;
    }

    static Set<AnnotationInstance> annotations(Set<AnnotationInstance> annotations) {
        return annotations(annotations, HashSet::new);
    }

    static List<AnnotationInstance> annotations(List<AnnotationInstance> annotations) {
        return annotations(annotations, ArrayList::new);
    }

    private static <C extends Collection<AnnotationInstance>> C annotations(C annotations, Supplier<C> creator) {
        if (annotations.size() <= 1) {
            return annotations;
        }

        C result = creator.get();
        Set<AnnotationInstanceEquivalenceProxy> seen = new HashSet<>((int) (annotations.size() / 0.75f) + 1);
        for (AnnotationInstance annotation : annotations) {
            if (seen.add(annotation.createEquivalenceProxy())) {
                result.add(annotation);
            }
        }
        return result;
    }

    static Set<StereotypeInfo> stereotypes(Set<StereotypeInfo> stereotypes) {
        return stereotypes(stereotypes, HashSet::new);
    }

    static List<StereotypeInfo> stereotypes(List<StereotypeInfo> stereotypes) {
        return stereotypes(stereotypes, ArrayList::new);
    }

    private static <C extends Collection<StereotypeInfo>> C stereotypes(C stereotypes, Supplier<C> creator) {
        if (stereotypes.size() <= 1) {
            return stereotypes;
        }

        C result = creator.get();
        Set<DotName> seen = new HashSet<>((int) (stereotypes.size() / 0.75f) + 1);
        for (StereotypeInfo stereotype : stereotypes) {
            if (seen.add(stereotype.getName())) {
                result.add(stereotype);
            }
        }
        return result;
    }
}
