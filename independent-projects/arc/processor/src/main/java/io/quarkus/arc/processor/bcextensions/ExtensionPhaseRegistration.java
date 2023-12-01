package io.quarkus.arc.processor.bcextensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.IndexView;

import io.quarkus.arc.processor.InterceptorInfo;

class ExtensionPhaseRegistration extends ExtensionPhaseBase {
    private final AllAnnotationOverlays annotationOverlays;
    private final Collection<io.quarkus.arc.processor.BeanInfo> allBeans;
    private final Collection<io.quarkus.arc.processor.InterceptorInfo> allInterceptors;
    private final Collection<io.quarkus.arc.processor.ObserverInfo> allObservers;
    private final io.quarkus.arc.processor.AssignabilityCheck assignability;

    ExtensionPhaseRegistration(ExtensionInvoker invoker, IndexView beanArchiveIndex, SharedErrors errors,
            AllAnnotationOverlays annotationOverlays, Collection<io.quarkus.arc.processor.BeanInfo> allBeans,
            Collection<InterceptorInfo> allInterceptors, Collection<io.quarkus.arc.processor.ObserverInfo> allObservers) {
        super(ExtensionPhase.REGISTRATION, invoker, beanArchiveIndex, errors);
        this.annotationOverlays = annotationOverlays;
        this.allBeans = allBeans;
        this.allInterceptors = allInterceptors;
        this.allObservers = allObservers;
        this.assignability = new io.quarkus.arc.processor.AssignabilityCheck(beanArchiveIndex, null);
    }

    void runExtensionMethod(ExtensionMethod method) throws ReflectiveOperationException {
        int numQueryParameters = 0;
        List<ExtensionMethodParameter> parameters = new ArrayList<>(method.parametersCount());
        for (org.jboss.jandex.Type parameterType : method.parameterTypes()) {
            ExtensionMethodParameter parameter = ExtensionMethodParameter.of(parameterType);
            parameters.add(parameter);

            if (parameter.isQuery()) {
                numQueryParameters++;
            }

            parameter.verifyAvailable(ExtensionPhase.REGISTRATION, method);
        }

        if (numQueryParameters == 0) {
            throw new DefinitionException("No parameter of type BeanInfo or ObserverInfo for method " + method);
        }

        if (numQueryParameters > 1) {
            throw new DefinitionException("More than 1 parameter of type BeanInfo or ObserverInfo for method " + method);
        }

        ExtensionMethodParameter query = parameters.stream()
                .filter(ExtensionMethodParameter::isQuery)
                .findAny()
                .get(); // guaranteed to be there

        List<?> allValuesForQueryParameter = Collections.emptyList();
        if (query == ExtensionMethodParameter.BEAN_INFO) {
            allValuesForQueryParameter = matchingBeans(method.jandex, false);
        } else if (query == ExtensionMethodParameter.INTERCEPTOR_INFO) {
            allValuesForQueryParameter = matchingBeans(method.jandex, true);
        } else if (query == ExtensionMethodParameter.OBSERVER_INFO) {
            allValuesForQueryParameter = matchingObservers(method.jandex);
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

    private Set<org.jboss.jandex.Type> expectedTypes(org.jboss.jandex.MethodInfo jandexMethod) {
        org.jboss.jandex.Type[] annotationValue = jandexMethod.annotation(DotNames.REGISTRATION)
                .value("types").asClassArray();
        return Set.of(annotationValue);
    }

    private List<BeanInfo> matchingBeans(org.jboss.jandex.MethodInfo jandexMethod, boolean onlyInterceptors) {
        Set<org.jboss.jandex.Type> expectedTypes = expectedTypes(jandexMethod);
        return Stream.concat(allBeans.stream(), allInterceptors.stream())
                .filter(bean -> {
                    if (onlyInterceptors && !bean.isInterceptor()) {
                        return false;
                    }
                    for (org.jboss.jandex.Type type : bean.getTypes()) {
                        if (expectedTypes.contains(type)) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(it -> BeanInfoImpl.create(index, annotationOverlays, it))
                .collect(Collectors.toUnmodifiableList());
    }

    private List<ObserverInfo> matchingObservers(org.jboss.jandex.MethodInfo jandexMethod) {
        Set<org.jboss.jandex.Type> expectedTypes = expectedTypes(jandexMethod);
        return allObservers.stream()
                .filter(observer -> {
                    org.jboss.jandex.Type observedType = observer.getObservedType();
                    for (org.jboss.jandex.Type expectedType : expectedTypes) {
                        if (assignability.isAssignableFrom(expectedType, observedType)) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(it -> new ObserverInfoImpl(index, annotationOverlays, it))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    Object argumentForExtensionMethod(ExtensionMethodParameter type, ExtensionMethod method) {
        if (type == ExtensionMethodParameter.TYPES) {
            return new TypesImpl(index, annotationOverlays);
        }

        return super.argumentForExtensionMethod(type, method);
    }
}
