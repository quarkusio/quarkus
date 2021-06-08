package io.quarkus.spring.di.deployment;

import static org.jboss.jandex.AnnotationInstance.create;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.DefinitionException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalStereotypeBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/*
 * A simple processor that maps annotations Spring DI annotation to CDI annotation
 * Arc's handling of annotation mapping (by creating an extra abstraction layer on top of the Jandex index)
 * suits this sort of handling perfectly
 */
public class SpringDIProcessor {

    private static final DotName SPRING_SCOPE_ANNOTATION = DotName.createSimple("org.springframework.context.annotation.Scope");

    static final DotName SPRING_COMPONENT = DotName.createSimple("org.springframework.stereotype.Component");
    static final DotName SPRING_SERVICE = DotName.createSimple("org.springframework.stereotype.Service");
    static final DotName SPRING_REPOSITORY = DotName.createSimple("org.springframework.stereotype.Repository");
    private static final Set<DotName> SPRING_STEREOTYPE_ANNOTATIONS = Arrays.stream(new DotName[] {
            SPRING_COMPONENT,
            SPRING_SERVICE,
            SPRING_REPOSITORY,
    }).collect(Collectors.toSet());

    private static final DotName CONFIGURATION_ANNOTATION = DotName
            .createSimple("org.springframework.context.annotation.Configuration");

    private static final DotName BEAN_ANNOTATION = DotName.createSimple("org.springframework.context.annotation.Bean");

    private static final DotName AUTOWIRED_ANNOTATION = DotName
            .createSimple("org.springframework.beans.factory.annotation.Autowired");

    private static final DotName SPRING_QUALIFIER_ANNOTATION = DotName
            .createSimple("org.springframework.beans.factory.annotation.Qualifier");

    private static final DotName SPRING_VALUE_ANNOTATION = DotName
            .createSimple("org.springframework.beans.factory.annotation.Value");

    private static final DotName CDI_SINGLETON_ANNOTATION = BuiltinScope.SINGLETON.getInfo().getDotName();
    private static final DotName CDI_DEPENDENT_ANNOTATION = BuiltinScope.DEPENDENT.getInfo().getDotName();
    private static final DotName CDI_REQUEST_SCOPED_ANNOTATION = BuiltinScope.REQUEST.getInfo().getDotName();
    private static final DotName CDI_SESSION_SCOPED_ANNOTATION = DotName.createSimple("javax.enterprise.context.SessionScoped");
    private static final DotName CDI_APP_SCOPED_ANNOTATION = BuiltinScope.APPLICATION.getInfo().getDotName();
    private static final DotName CDI_NAMED_ANNOTATION = DotNames.NAMED;
    private static final DotName CDI_INJECT_ANNOTATION = DotNames.INJECT;
    private static final DotName CDI_PRODUCES_ANNOTATION = DotNames.PRODUCES;
    private static final DotName MP_CONFIG_PROPERTY_ANNOTATION = DotName.createSimple(ConfigProperty.class.getName());

    @BuildStep
    FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(Feature.SPRING_DI);
    }

    /*
     * This Build Item can't be generated in the beanTransformer method because the annotation transformer
     * is generated lazily.
     * However the logic is the same
     */
    @BuildStep
    SpringBeanNameToDotNameBuildItem createBeanNamesMap(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) {
        final Map<String, DotName> result = new HashMap<>();

        final IndexView index = beanArchiveIndexBuildItem.getIndex();
        final Collection<AnnotationInstance> stereotypeInstances = new ArrayList<>();
        stereotypeInstances.addAll(index.getAnnotations(SPRING_COMPONENT));
        stereotypeInstances.addAll(index.getAnnotations(SPRING_REPOSITORY));
        stereotypeInstances.addAll(index.getAnnotations(SPRING_SERVICE));
        for (AnnotationInstance stereotypeInstance : stereotypeInstances) {
            if (stereotypeInstance.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            result.put(getBeanNameFromStereotypeInstance(stereotypeInstance), stereotypeInstance.target().asClass().name());
        }

        for (AnnotationInstance beanInstance : index.getAnnotations(BEAN_ANNOTATION)) {
            if (beanInstance.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            result.put(getBeanNameFromBeanInstance(beanInstance), beanInstance.target().asMethod().returnType().name());
        }

        return new SpringBeanNameToDotNameBuildItem(result);
    }

    @BuildStep
    AnnotationsTransformerBuildItem beanTransformer(
            final BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            final BuildProducer<AdditionalStereotypeBuildItem> additionalStereotypeBuildItemBuildProducer) {
        final IndexView index = beanArchiveIndexBuildItem.getIndex();
        final Map<DotName, Set<DotName>> stereotypeScopes = getStereotypeScopes(index);
        final Map<DotName, Collection<AnnotationInstance>> instances = new HashMap<>();
        for (final DotName name : stereotypeScopes.keySet()) {
            instances.put(name, index.getAnnotations(name)
                    .stream()
                    .filter(it -> it.target().kind() == AnnotationTarget.Kind.CLASS
                            && it.target().asClass().isAnnotation())
                    .collect(Collectors.toSet()));
        }
        additionalStereotypeBuildItemBuildProducer.produce(new AdditionalStereotypeBuildItem(instances));
        return new AnnotationsTransformerBuildItem(context -> {
            final Collection<AnnotationInstance> annotations = context.getAnnotations();
            if (annotations.isEmpty()) {
                return;
            }
            final AnnotationTarget target = context.getTarget();
            // Note that only built-in scopes are used because annotation transformers can be used before custom contexts are registered
            final Set<AnnotationInstance> annotationsToAdd = getAnnotationsToAdd(target, stereotypeScopes,
                    Arrays.stream(BuiltinScope.values()).map(BuiltinScope::getName).collect(Collectors.toList()));
            if (!annotationsToAdd.isEmpty()) {
                final Transformation transform = context.transform();
                for (AnnotationInstance annotationInstance : annotationsToAdd) {
                    transform.add(annotationInstance);
                }
                transform.done();
            }
        });
    }

    /**
     * @param index An index view of the archive including all the spring classes
     * @return A map of any spring annotations extending @Component which will function like
     *         CDI stereotypes to any scopes it, or any of its stereotypes declared.
     */
    Map<DotName, Set<DotName>> getStereotypeScopes(final IndexView index) {
        final Map<DotName, Set<DotName>> scopes = new HashMap<>();
        scopes.put(SPRING_COMPONENT, Collections.emptySet());
        scopes.put(SPRING_REPOSITORY, Collections.emptySet());
        scopes.put(SPRING_SERVICE, Collections.emptySet());
        final List<ClassInfo> allAnnotations = getOrderedAnnotations(index);
        final Set<DotName> stereotypeClasses = new HashSet<>(SPRING_STEREOTYPE_ANNOTATIONS);
        for (final ClassInfo clazz : allAnnotations) {
            final Set<DotName> clazzAnnotations = clazz.annotations().keySet();
            final Set<DotName> clazzStereotypes = new HashSet<>();
            final Set<DotName> clazzScopes = new HashSet<>();
            for (final DotName stereotypeName : stereotypeClasses) {
                if (clazzAnnotations.contains(stereotypeName)) {
                    clazzStereotypes.add(stereotypeName);
                }
            }
            if (clazzStereotypes.isEmpty()) {
                continue;
            }
            final DotName name = clazz.name();
            stereotypeClasses.add(name);
            for (final DotName stereotype : clazzStereotypes) {
                final Set<DotName> vals = scopes.get(stereotype);
                if (vals != null) {
                    clazzScopes.addAll(vals);
                }
            }
            final DotName scope = getScope(clazz);
            if (scope != null) {
                clazzScopes.add(scope);
            }
            scopes.put(name, clazzScopes);
        }
        return scopes;
    }

    /**
     * Translate spring built-in scope identifiers to CDI scopes.
     *
     * @param target The annotated element declaring the @Scope
     * @return A CDI built in (or session) scope that mostly matches
     *         the spring one. Websocket scope is currently mapped to @Dependent
     *         and spring custom scopes are not currently handled.
     */
    private DotName getScope(final AnnotationTarget target) {
        AnnotationValue value = null;
        if (target.kind() == AnnotationTarget.Kind.CLASS) {
            if (target.asClass().classAnnotation(SPRING_SCOPE_ANNOTATION) != null) {
                value = target.asClass().classAnnotation(SPRING_SCOPE_ANNOTATION).value();
                if ((value == null) || value.asString().isEmpty()) {
                    value = target.asClass().classAnnotation(SPRING_SCOPE_ANNOTATION).value("scopeName");
                }
            }
        } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
            if (target.asMethod().hasAnnotation(SPRING_SCOPE_ANNOTATION)) {
                value = target.asMethod().annotation(SPRING_SCOPE_ANNOTATION).value();
                if ((value == null) || value.asString().isEmpty()) {
                    value = target.asMethod().annotation(SPRING_SCOPE_ANNOTATION).value("scopeName");
                }
            }
        }
        if (value != null) {
            switch (value.asString()) {
                case "singleton":
                    return CDI_SINGLETON_ANNOTATION;
                case "request":
                    return CDI_REQUEST_SCOPED_ANNOTATION;
                case "global session":
                case "application":
                    return CDI_APP_SCOPED_ANNOTATION;
                case "session":
                    return CDI_SESSION_SCOPED_ANNOTATION;
                case "websocket":
                case "prototype":
                    return CDI_DEPENDENT_ANNOTATION;
            }
        }
        return null;
    }

    /**
     * @return All the annotations in the index ordered with dependents after their dependencies.
     *         They are traversed in this order so that we can start with out root annotations
     *         (@Component, @Service & @Repository) and create a set of everything that extends it
     *         directly or indirectly.
     */
    private List<ClassInfo> getOrderedAnnotations(final IndexView index) {
        final Map<DotName, Set<DotName>> deps = new HashMap<>();
        for (final ClassInfo clazz : index.getKnownClasses()) {
            if (clazz.isAnnotation()) {
                deps.put(clazz.name(), clazz.annotations().keySet());
            }
        }
        final List<ClassInfo> ret = new ArrayList<>();
        final Set<DotName> visited = new HashSet<>();
        for (final DotName clazz : deps.keySet()) {
            visitAnnotation(clazz, index, deps, visited, ret);
        }
        return ret;
    }

    private void visitAnnotation(final DotName clazz, final IndexView index, final Map<DotName, Set<DotName>> deps,
            final Set<DotName> visited, final List<ClassInfo> ret) {
        if (visited.contains(clazz)) {
            return;
        }
        visited.add(clazz);
        final Set<DotName> annotations = deps.get(clazz);
        if (annotations != null) {
            for (final DotName annotation : annotations) {
                visitAnnotation(annotation, index, deps, visited, ret);
            }
        }
        final ClassInfo classInfo = index.getClassByName(clazz);
        if (classInfo != null) {
            ret.add(classInfo);
        }
    }

    /**
     * Map spring annotations from an annotated class to equivalent CDI annotations
     *
     * @param target The annotated class
     * @param stereotypeScopes A map on spring stereotype classes to all the scopes
     *        they, or any of their stereotypes (etc) declare
     * @return The CDI annotations to add to the class
     */
    Set<AnnotationInstance> getAnnotationsToAdd(
            final AnnotationTarget target,
            final Map<DotName, Set<DotName>> stereotypeScopes,
            final List<DotName> allArcScopes) {
        List<DotName> arcScopes = allArcScopes != null ? allArcScopes
                : Arrays.stream(BuiltinScope.values()).map(i -> i.getName()).collect(Collectors.toList());
        final Set<DotName> stereotypes = stereotypeScopes.keySet();

        final Set<AnnotationInstance> annotationsToAdd = new HashSet<>();

        //if it's a class, it's a Bean or a Bean producer
        if (target.kind() == AnnotationTarget.Kind.CLASS) {
            final ClassInfo classInfo = target.asClass();
            final Set<DotName> scopes = new HashSet<>();
            final Set<DotName> scopeStereotypes = new HashSet<>();
            final Set<String> names = new HashSet<>();
            final Set<DotName> clazzAnnotations = classInfo.annotations().keySet();

            for (AnnotationInstance instance : classInfo.classAnnotations()) {
                // make sure that we don't mix and match Spring and CDI annotations since this can cause a lot of problems
                if (arcScopes.contains(instance.name())) {
                    return annotationsToAdd;
                }
            }

            for (final DotName clazzAnnotation : clazzAnnotations) {
                if (stereotypes.contains(clazzAnnotation)) {
                    scopeStereotypes.add(clazzAnnotation);
                    final Set<DotName> scopeNames = stereotypeScopes.get(clazzAnnotation);
                    if (scopeNames != null) {
                        scopes.addAll(scopeNames);
                    }
                    if (SPRING_STEREOTYPE_ANNOTATIONS.contains(clazzAnnotation) && !classInfo.isAnnotation()) {
                        names.add(getBeanNameFromStereotypeInstance(classInfo.classAnnotation(clazzAnnotation)));
                    }
                }
            }
            DotName declaredScope = getScope(classInfo);
            // @Named is a bean-defining annotation in Spring, but not in Arc.
            if (declaredScope == null && classInfo.classAnnotation(CDI_NAMED_ANNOTATION) != null) {
                declaredScope = CDI_SINGLETON_ANNOTATION; // implicit default scope in spring
            }
            final boolean isAnnotation = classInfo.isAnnotation();
            if (declaredScope != null) {
                annotationsToAdd.add(create(
                        declaredScope,
                        target,
                        Collections.emptyList()));
            } else if (!(isAnnotation && scopes.isEmpty()) && !classInfo.annotations().containsKey(CONFIGURATION_ANNOTATION)) { // Annotations without an explicit scope shouldn't default to anything
                boolean shouldAdd = false;
                for (final DotName clazzAnnotation : clazzAnnotations) {
                    if (stereotypes.contains(clazzAnnotation) || scopeStereotypes.contains(clazzAnnotation)) {
                        shouldAdd = true;
                        break;
                    }
                }

                if (shouldAdd) {
                    final DotName scope = validateScope(classInfo, scopes, scopeStereotypes);
                    annotationsToAdd.add(create(
                            scope,
                            target,
                            Collections.emptyList()));
                }
            }
            final String name = validateName(classInfo, names);
            if (name != null) {
                annotationsToAdd.add(create(
                        CDI_NAMED_ANNOTATION,
                        target,
                        Collections.singletonList(AnnotationValue.createStringValue("value", name))));
            }

            if (classInfo.annotations().containsKey(CONFIGURATION_ANNOTATION)) {
                annotationsToAdd.add(create(
                        CDI_APP_SCOPED_ANNOTATION,
                        target,
                        Collections.emptyList()));
            }
            if (isAnnotation && !annotationsToAdd.isEmpty()) {
                annotationsToAdd.add(create(
                        DotNames.STEREOTYPE,
                        target,
                        Collections.emptyList()));
            }
        } else if (target.kind() == AnnotationTarget.Kind.FIELD) { // here we check for @Autowired and @Value annotations
            final FieldInfo fieldInfo = target.asField();
            if (fieldInfo.hasAnnotation(AUTOWIRED_ANNOTATION)) {
                annotationsToAdd.add(create(
                        CDI_INJECT_ANNOTATION,
                        target,
                        Collections.emptyList()));

                if (fieldInfo.hasAnnotation(SPRING_QUALIFIER_ANNOTATION)) {
                    final AnnotationInstance annotation = fieldInfo.annotation(SPRING_QUALIFIER_ANNOTATION);
                    final AnnotationValue annotationValue = annotation.value();
                    if (annotationValue != null) {
                        final String value = annotationValue.asString();
                        annotationsToAdd.add(create(
                                CDI_NAMED_ANNOTATION,
                                target,
                                Collections.singletonList((AnnotationValue.createStringValue("value", value)))));
                    }
                }
            } else if (fieldInfo.hasAnnotation(SPRING_VALUE_ANNOTATION)) {
                final AnnotationInstance annotation = fieldInfo.annotation(SPRING_VALUE_ANNOTATION);
                addSpringValueAnnotations(target, annotation, true, annotationsToAdd);
            }
        } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
            final MethodInfo methodInfo = target.asMethod();
            if (methodInfo.hasAnnotation(BEAN_ANNOTATION)
                    && methodInfo.declaringClass().classAnnotation(CONFIGURATION_ANNOTATION) != null) {
                annotationsToAdd.add(create(
                        CDI_PRODUCES_ANNOTATION,
                        target,
                        Collections.emptyList()));
                DotName declaredScope = getScope(methodInfo);
                // Non-spring specific annotations are processed by Arc
                if (declaredScope == null && !methodInfo.hasAnnotation(CDI_SINGLETON_ANNOTATION)) {
                    declaredScope = CDI_SINGLETON_ANNOTATION; // implicit default scope in spring
                }
                if (declaredScope != null) {
                    annotationsToAdd.add(create(
                            declaredScope,
                            target,
                            Collections.emptyList()));
                }

                String beanName = getBeanNameFromBeanInstance(methodInfo.annotation(BEAN_ANNOTATION));
                annotationsToAdd.add(create(
                        CDI_NAMED_ANNOTATION,
                        target,
                        Collections.singletonList(AnnotationValue.createStringValue("value", beanName))));
            } else if (methodInfo.hasAnnotation(AUTOWIRED_ANNOTATION)) {
                annotationsToAdd.add(create(
                        CDI_INJECT_ANNOTATION,
                        target,
                        Collections.emptyList()));
            }

            // add method parameter conversion annotations
            for (AnnotationInstance annotation : methodInfo.annotations()) {
                if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    if (annotation.name().equals(SPRING_QUALIFIER_ANNOTATION)) {
                        final AnnotationValue annotationValue = annotation.value();
                        if (annotationValue != null) {
                            final String value = annotationValue.asString();
                            annotationsToAdd.add(create(
                                    CDI_NAMED_ANNOTATION,
                                    annotation.target(),
                                    Collections.singletonList(AnnotationValue.createStringValue("value", value))));
                        }
                    } else if (annotation.name().equals(SPRING_VALUE_ANNOTATION)) {
                        addSpringValueAnnotations(annotation.target(), annotation, false, annotationsToAdd);
                    }
                }
            }

        }
        return annotationsToAdd;
    }

    /**
     * Meant to be called with instances of @Component, @Service, @Repository
     */
    private String getBeanNameFromStereotypeInstance(AnnotationInstance annotationInstance) {
        if (annotationInstance.target().kind() != AnnotationTarget.Kind.CLASS) {
            throw new IllegalStateException(
                    "AnnotationInstance " + annotationInstance + " is an invalid target. Only Class targets are supported");
        }
        final AnnotationValue value = annotationInstance.value();
        if ((value == null) || value.asString().isEmpty()) {
            return getDefaultBeanNameFromClass(annotationInstance.target().asClass().name().toString());
        } else {
            return value.asString();
        }
    }

    /**
     * Meant to be called with instances of @Bean
     */
    private String getBeanNameFromBeanInstance(AnnotationInstance annotationInstance) {
        if (annotationInstance.target().kind() != AnnotationTarget.Kind.METHOD) {
            throw new IllegalStateException(
                    "AnnotationInstance " + annotationInstance + " is an invalid target. Only Method targets are supported");
        }

        String beanName = null;
        final AnnotationValue beanNameAnnotationValue = annotationInstance.value("name");
        if (beanNameAnnotationValue != null) {
            beanName = determineName(beanNameAnnotationValue);
        }
        if (beanName == null || beanName.isEmpty()) {
            final AnnotationValue beanValueAnnotationValue = annotationInstance.value();
            if (beanNameAnnotationValue != null) {
                beanName = determineName(beanValueAnnotationValue);
            }
        }
        if (beanName == null || beanName.isEmpty()) {
            beanName = annotationInstance.target().asMethod().name();
        }

        return beanName;
    }

    // this does what Spring's AnnotationBeanNameGenerator does to generate a name
    private String getDefaultBeanNameFromClass(String className) {
        return decapitalize(getShortNameOfClass(className));
    }

    private String getShortNameOfClass(String className) {
        int lastDotIndex = className.lastIndexOf('.');
        int nameEndIndex = className.indexOf("$$");
        if (nameEndIndex == -1) {
            nameEndIndex = className.length();
        }
        String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
        shortName = shortName.replace('$', '.');
        return shortName;
    }

    private String decapitalize(String name) {
        if (name != null && name.length() != 0) {
            if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
                return name;
            } else {
                char[] chars = name.toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                return new String(chars);
            }
        } else {
            return name;
        }
    }

    private void addSpringValueAnnotations(AnnotationTarget target, AnnotationInstance annotation, boolean addInject,
            Set<AnnotationInstance> annotationsToAdd) {
        final AnnotationValue annotationValue = annotation.value();
        if (annotationValue == null) {
            return;
        }
        String defaultValue = null;
        String propertyName = annotationValue.asString().replace("${", "").replace("}", "");
        if (propertyName.contains(":")) {
            final int index = propertyName.indexOf(':');
            if (index < propertyName.length() - 1) {
                defaultValue = propertyName.substring(index + 1);
            }
            propertyName = propertyName.substring(0, index);

        }
        final List<AnnotationValue> annotationValues = new ArrayList<>();
        annotationValues.add(AnnotationValue.createStringValue("name", propertyName));
        if (defaultValue != null && !defaultValue.isEmpty()) {
            annotationValues.add(AnnotationValue.createStringValue("defaultValue", defaultValue));
        }
        annotationsToAdd.add(create(
                MP_CONFIG_PROPERTY_ANNOTATION,
                target,
                annotationValues));
        if (addInject) {
            annotationsToAdd.add(create(
                    CDI_INJECT_ANNOTATION,
                    target,
                    Collections.emptyList()));
        }
    }

    private static String determineName(AnnotationValue annotationValue) {
        if (annotationValue.kind() == AnnotationValue.Kind.ARRAY) {
            return annotationValue.asStringArray()[0];
        } else if (annotationValue.kind() == AnnotationValue.Kind.STRING) {
            return annotationValue.asString();
        }
        return null;
    }

    /**
     * Get a single scope from the available options or throw a {@link DefinitionException} explaining
     * where the annotations conflict.
     *
     * @param clazz The class annotated with the scopes
     * @param scopes The scopes from the class and its stereotypes
     * @param scopeStereotypes The stereotype annotations that declared the conflicting scopes
     * @return The scope for the target class
     */
    private DotName validateScope(final ClassInfo clazz, final Set<DotName> scopes, final Set<DotName> scopeStereotypes) {
        final int size = scopes.size();
        switch (size) {
            case 0:
                // Spring default
                return CDI_SINGLETON_ANNOTATION;
            case 1:
                return scopes.iterator().next();
            default:
                throw new DefinitionException(
                        "Components annotated with multiple conflicting scopes must declare an explicit @Scope. "
                                + clazz.name() + " declares scopes: "
                                + scopes.stream().map(DotName::toString).collect(Collectors.joining(", "))
                                + " through the stereotypes: "
                                + scopeStereotypes.stream().map(DotName::toString)
                                        .collect(Collectors.joining(", ")));
        }
    }

    /**
     * Get the name of a bean or throw a {@link DefinitionException} if it has more than one name
     *
     * @param clazz The class annotated with the names
     * @param names The names
     * @return The bane name
     */
    private String validateName(final ClassInfo clazz, final Set<String> names) {
        final int size = names.size();
        switch (size) {
            case 0:
                return null;
            case 1:
                return names.iterator().next();
            default:
                throw new DefinitionException(
                        "Component " + clazz.name() + " is annotated with multiple conflicting names: "
                                + String.join(", ", names));
        }
    }
}
