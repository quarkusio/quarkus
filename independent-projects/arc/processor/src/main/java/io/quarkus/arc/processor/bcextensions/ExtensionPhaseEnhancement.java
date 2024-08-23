package io.quarkus.arc.processor.bcextensions;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.DotName;

class ExtensionPhaseEnhancement extends ExtensionPhaseBase {
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;

    ExtensionPhaseEnhancement(ExtensionInvoker invoker, org.jboss.jandex.IndexView beanArchiveIndex,
            SharedErrors errors, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay) {
        super(ExtensionPhase.ENHANCEMENT, invoker, beanArchiveIndex, errors);
        this.annotationOverlay = annotationOverlay;
    }

    @Override
    void runExtensionMethod(ExtensionMethod method) throws ReflectiveOperationException {
        int numQueryParameters = 0;
        List<ExtensionMethodParameter> parameters = new ArrayList<>(method.parametersCount());
        for (org.jboss.jandex.Type parameterType : method.parameterTypes()) {
            ExtensionMethodParameter parameter = ExtensionMethodParameter.of(parameterType);
            parameters.add(parameter);

            if (parameter.isQuery()) {
                numQueryParameters++;
            }

            parameter.verifyAvailable(ExtensionPhase.ENHANCEMENT, method);
        }

        if (numQueryParameters == 0) {
            throw new DefinitionException("No parameter of type ClassInfo, MethodInfo, FieldInfo, "
                    + "ClassConfig, MethodConfig, or FieldConfig for method " + method);
        }

        if (numQueryParameters > 1) {
            throw new DefinitionException("More than 1 parameter of type ClassInfo, MethodInfo, FieldInfo, "
                    + "ClassConfig, MethodConfig, or FieldConfig for method " + method);
        }

        ExtensionMethodParameter query = parameters.stream()
                .filter(ExtensionMethodParameter::isQuery)
                .findAny()
                .get(); // guaranteed to be there

        List<org.jboss.jandex.ClassInfo> matchingClasses = matchingClasses(method.jandex);
        List<?> allValuesForQueryParameter;
        if (query == ExtensionMethodParameter.CLASS_INFO) {
            allValuesForQueryParameter = matchingClasses.stream()
                    .map(it -> new ClassInfoImpl(index, annotationOverlay, it))
                    .toList();
        } else if (query == ExtensionMethodParameter.METHOD_INFO) {
            allValuesForQueryParameter = matchingClasses.stream()
                    .map(it -> new ClassInfoImpl(index, annotationOverlay, it))
                    .flatMap(it -> Stream.concat(it.constructors().stream(), it.methods().stream()))
                    .toList();
        } else if (query == ExtensionMethodParameter.FIELD_INFO) {
            allValuesForQueryParameter = matchingClasses.stream()
                    .map(it -> new ClassInfoImpl(index, annotationOverlay, it))
                    .flatMap(it -> it.fields().stream())
                    .toList();
        } else if (query == ExtensionMethodParameter.CLASS_CONFIG) {
            allValuesForQueryParameter = matchingClasses.stream()
                    .map(it -> new ClassConfigImpl(index, annotationOverlay, it))
                    .toList();
        } else if (query == ExtensionMethodParameter.METHOD_CONFIG) {
            allValuesForQueryParameter = matchingClasses.stream()
                    .map(it -> new ClassConfigImpl(index, annotationOverlay, it))
                    .flatMap(it -> Stream.concat(it.constructors().stream(), it.methods().stream()))
                    .toList();
        } else if (query == ExtensionMethodParameter.FIELD_CONFIG) {
            allValuesForQueryParameter = matchingClasses.stream()
                    .map(it -> new ClassConfigImpl(index, annotationOverlay, it))
                    .flatMap(it -> it.fields().stream())
                    .toList();
        } else {
            throw new IllegalArgumentException("Unknown query parameter " + query);
        }

        for (Object queryParameterValue : allValuesForQueryParameter) {
            List<Object> arguments = new ArrayList<>();
            for (ExtensionMethodParameter parameter : parameters) {
                Object argument = parameter.isQuery()
                        ? queryParameterValue
                        : argumentForExtensionMethod(parameter, method);
                arguments.add(argument);
            }

            util.callExtensionMethod(method, arguments);
        }
    }

    private List<org.jboss.jandex.ClassInfo> matchingClasses(org.jboss.jandex.MethodInfo method) {
        org.jboss.jandex.AnnotationInstance enhancement = method.annotation(DotNames.ENHANCEMENT);

        org.jboss.jandex.Type[] jandexTypes = enhancement.value("types").asClassArray();
        boolean withSubtypes = enhancement.valueWithDefault(index, "withSubtypes").asBoolean();

        List<org.jboss.jandex.ClassInfo> result = new ArrayList<>();
        for (org.jboss.jandex.Type jandexType : jandexTypes) {
            org.jboss.jandex.ClassInfo clazz = index.getClassByName(jandexType.name());
            if (clazz.isAnnotation()) {
                // we don't want to allow transforming annotations on annotations
                continue;
            }
            result.add(clazz);

            if (withSubtypes) {
                Collection<org.jboss.jandex.ClassInfo> subtypes = Modifier.isInterface(clazz.flags())
                        ? index.getAllKnownImplementors(jandexType.name())
                        : index.getAllKnownSubclasses(jandexType.name());
                result.addAll(subtypes);
            }
        }

        Set<DotName> withAnnotations = new HashSet<>();
        for (org.jboss.jandex.Type annotationType : enhancement.valueWithDefault(index, "withAnnotations").asClassArray()) {
            withAnnotations.add(annotationType.asClassType().name());
        }

        if (withAnnotations.isEmpty() || withAnnotations.contains(DotNames.ANNOTATION)) {
            return Collections.unmodifiableList(result);
        }

        List<org.jboss.jandex.ClassInfo> filteredResult = new ArrayList<>();
        for (org.jboss.jandex.ClassInfo clazz : result) {
            if (isAnyAnnotationPresent(withAnnotations, clazz, new HashSet<>())) {
                filteredResult.add(clazz);
            }
        }
        return Collections.unmodifiableList(filteredResult);
    }

    private boolean isAnyAnnotationPresent(Set<DotName> annotationNames, org.jboss.jandex.ClassInfo clazz,
            Set<DotName> alreadyProcessed) {
        Set<DotName> annotationsOnClass = clazz.annotationsMap().keySet();
        for (DotName annotationOnClass : annotationsOnClass) {
            if (alreadyProcessed.contains(annotationOnClass)) {
                continue;
            }
            alreadyProcessed.add(annotationOnClass);

            if (annotationNames.contains(annotationOnClass)) {
                return true;
            }

            org.jboss.jandex.ClassInfo annotationDeclaration = index.getClassByName(annotationOnClass);
            if (annotationDeclaration != null
                    && isAnyAnnotationPresent(annotationNames, annotationDeclaration, alreadyProcessed)) {
                return true;
            }
        }
        return false;
    }

    @Override
    Object argumentForExtensionMethod(ExtensionMethodParameter type, ExtensionMethod method) {
        if (type == ExtensionMethodParameter.TYPES) {
            return new TypesImpl(index, annotationOverlay);
        }

        return super.argumentForExtensionMethod(type, method);
    }
}
