package io.quarkus.arc.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class AutoAddScopeProcessor {

    private static final Logger LOGGER = Logger.getLogger(AutoAddScopeProcessor.class);

    @BuildStep
    void annotationTransformer(List<AutoAddScopeBuildItem> autoScopes, CustomScopeAnnotationsBuildItem scopes,
            List<AutoInjectAnnotationBuildItem> autoInjectAnnotations,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformers,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BeanArchiveIndexBuildItem beanArchiveIndex) throws Exception {
        if (autoScopes.isEmpty()) {
            return;
        }
        Set<DotName> containerAnnotationNames = autoInjectAnnotations.stream().flatMap(a -> a.getAnnotationNames().stream())
                .collect(Collectors.toSet());
        containerAnnotationNames.add(DotNames.POST_CONSTRUCT);
        containerAnnotationNames.add(DotNames.PRE_DESTROY);
        containerAnnotationNames.add(DotNames.INJECT);

        Set<DotName> unremovables = new HashSet<>();

        annotationsTransformers.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                if (scopes.isScopeIn(context.getAnnotations())) {
                    // Skip classes annotated with a scope
                    return;
                }
                ClassInfo clazz = context.getTarget().asClass();
                Boolean requiresContainerServices = null;

                for (AutoAddScopeBuildItem autoScope : autoScopes) {
                    if (autoScope.isContainerServicesRequired()) {
                        if (requiresContainerServices == null) {
                            requiresContainerServices = requiresContainerServices(clazz, containerAnnotationNames,
                                    beanArchiveIndex.getIndex());
                        }
                        if (!requiresContainerServices) {
                            // Skip - no injection point detected
                            continue;
                        }
                    }
                    if (autoScope.test(clazz, context.getAnnotations(), beanArchiveIndex.getIndex())) {
                        context.transform().add(autoScope.getDefaultScope()).done();
                        if (autoScope.isUnremovable()) {
                            unremovables.add(clazz.name());
                        }
                        LOGGER.debugf("Automatically added scope %s to class %s" + autoScope.getReason(),
                                autoScope.getDefaultScope(), clazz, autoScope.getReason());
                        break;
                    }
                }
            }
        }));

        if (!unremovables.isEmpty()) {
            unremovableBeans.produce(new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {

                @Override
                public boolean test(BeanInfo bean) {
                    return bean.isClassBean() && unremovables.contains(bean.getBeanClass());
                }
            }));
        }
    }

    private boolean requiresContainerServices(ClassInfo clazz, Set<DotName> containerAnnotationNames, IndexView index) {
        // Note that transformed methods/fields are not taken into account
        if (hasContainerAnnotation(clazz, containerAnnotationNames)) {
            return true;
        }
        if (index != null) {
            DotName superName = clazz.superName();
            while (superName != null && !superName.equals(DotNames.OBJECT)) {
                final ClassInfo superClass = index.getClassByName(superName);
                if (superClass != null) {
                    if (hasContainerAnnotation(superClass, containerAnnotationNames)) {
                        return true;
                    }
                    superName = superClass.superName();
                } else {
                    superName = null;
                }
            }
        }
        return false;
    }

    private boolean hasContainerAnnotation(ClassInfo clazz, Set<DotName> containerAnnotationNames) {
        if (clazz.annotations().isEmpty() || containerAnnotationNames.isEmpty()) {
            return false;
        }
        return containsAny(clazz, containerAnnotationNames);
    }

    private boolean containsAny(ClassInfo clazz, Set<DotName> annotationNames) {
        for (DotName annotation : clazz.annotations().keySet()) {
            if (annotationNames.contains(annotation)) {
                return true;
            }
        }
        return false;
    }

}
