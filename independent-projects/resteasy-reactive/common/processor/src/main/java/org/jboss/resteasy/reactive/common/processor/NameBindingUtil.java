package org.jboss.resteasy.reactive.common.processor;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONSUMES;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.NAME_BINDING;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATH;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PRODUCES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

public class NameBindingUtil {

    // this is used to avoid looking up annotation that don't exist in an index and can cause
    // annoying indexing warnings.
    // TODO: We might have to make this smarter at some point
    private static final Predicate<DotName> CLASS_ANNOTATION_SKIP_PREDICATE = new Predicate<>() {
        @Override
        public boolean test(DotName dotName) {
            String className = dotName.toString();
            return className.contains("NonNull") || className.startsWith("lombok");
        }
    };

    /**
     * Returns the class names of the {@code @NameBinding} annotations or null if non are present
     */
    public static Set<String> nameBindingNames(IndexView index, ClassInfo classInfo) {
        return nameBindingNames(index, instanceDotNames(classInfo.declaredAnnotations()));
    }

    public static Set<String> nameBindingNames(IndexView index, MethodInfo methodInfo, Set<String> forClass) {
        Set<String> fromMethod = nameBindingNames(index, instanceDotNames(methodInfo.annotations()));
        if (fromMethod.isEmpty()) {
            return forClass;
        }
        fromMethod.addAll(forClass);
        return fromMethod;
    }

    private static List<DotName> instanceDotNames(Collection<AnnotationInstance> instances) {
        List<DotName> result = new ArrayList<>(instances.size());
        for (AnnotationInstance instance : instances) {
            result.add(instance.name());
        }
        return result;
    }

    private static Set<String> nameBindingNames(IndexView index, Collection<DotName> annotations) {
        Set<String> result = new HashSet<>();
        for (DotName classAnnotationDotName : annotations) {
            if (classAnnotationDotName.equals(PATH) || classAnnotationDotName.equals(CONSUMES)
                    || classAnnotationDotName.equals(PRODUCES)) {
                continue;
            }
            if (CLASS_ANNOTATION_SKIP_PREDICATE.test(classAnnotationDotName)) {
                continue;
            }
            ClassInfo classAnnotation = index.getClassByName(classAnnotationDotName);
            if (classAnnotation == null) {
                continue;
            }
            if (classAnnotation.declaredAnnotation(NAME_BINDING) != null) {
                result.add(classAnnotation.name().toString());
            }
        }
        return result;
    }
}
