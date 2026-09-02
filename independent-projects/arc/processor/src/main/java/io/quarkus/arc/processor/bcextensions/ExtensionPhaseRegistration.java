package io.quarkus.arc.processor.bcextensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.inject.spi.DefinitionException;

import io.quarkus.arc.processor.InterceptorInfo;
import io.quarkus.arc.processor.JandexTypeSystem;
import io.quarkus.arc.processor.Types;

class ExtensionPhaseRegistration extends ExtensionPhaseBase {
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    private final Collection<io.quarkus.arc.processor.BeanInfo> allBeans;
    private final Collection<io.quarkus.arc.processor.InterceptorInfo> allInterceptors;
    private final Collection<io.quarkus.arc.processor.ObserverInfo> allObservers;
    private final io.quarkus.arc.processor.InvokerFactory invokerFactory;

    ExtensionPhaseRegistration(ExtensionInvoker invoker, org.jboss.jandex.IndexView beanArchiveIndex, SharedErrors errors,
            org.jboss.jandex.MutableAnnotationOverlay annotationOverlay, Collection<io.quarkus.arc.processor.BeanInfo> allBeans,
            Collection<InterceptorInfo> allInterceptors, Collection<io.quarkus.arc.processor.ObserverInfo> allObservers,
            io.quarkus.arc.processor.InvokerFactory invokerFactory) {
        super(ExtensionPhase.REGISTRATION, invoker, beanArchiveIndex, errors);
        this.annotationOverlay = annotationOverlay;
        this.allBeans = allBeans;
        this.allInterceptors = allInterceptors;
        this.allObservers = allObservers;
        this.invokerFactory = invokerFactory;
    }

    void runExtensionMethod(ExtensionMethod method) throws Exception {
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
            allValuesForQueryParameter = matchingBeans(method, false);
        } else if (query == ExtensionMethodParameter.INTERCEPTOR_INFO) {
            allValuesForQueryParameter = matchingBeans(method, true);
        } else if (query == ExtensionMethodParameter.OBSERVER_INFO) {
            allValuesForQueryParameter = matchingObservers(method);
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

    private Set<org.jboss.jandex.Type> expectedTypes(ExtensionMethod method) {
        Set<org.jboss.jandex.Type> result = new HashSet<>();
        org.jboss.jandex.Type[] types = method.jandex.annotation(DotNames.REGISTRATION)
                .value("types").asClassArray();
        for (org.jboss.jandex.Type type : types) {
            org.jboss.jandex.ClassInfo clazz = index.getClassByName(type.name());
            if (clazz != null && DotNames.TYPE_LITERAL.equals(clazz.superName())) {
                org.jboss.jandex.Type typeLiteral = clazz.superClassType();
                if (typeLiteral.kind() != org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE) {
                    throw new DefinitionException("Raw TypeLiteral in @Registration.types on " + method);
                }
                org.jboss.jandex.Type typeArg = typeLiteral.asParameterizedType().arguments().get(0);
                if (Types.containsTypeVariable(typeArg)) {
                    throw new DefinitionException("Type variable in @Registration.types on " + method);
                }
                if (Types.containsWildcard(typeArg, null, false)) {
                    throw new DefinitionException("Wildcard in @Registration.types on " + method
                            + " (this makes no sense for beans and is unspecified for observers)");
                }
                result.add(typeArg);
            } else {
                result.add(type);
            }
        }
        return result;
    }

    private List<BeanInfo> matchingBeans(ExtensionMethod method, boolean onlyInterceptors) {
        Set<org.jboss.jandex.Type> expectedTypes = expectedTypes(method);
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
                .map(it -> (BeanInfo) BeanInfoImpl.create(index, annotationOverlay, it))
                .toList();
    }

    private List<ObserverInfo> matchingObservers(ExtensionMethod method) {
        Set<org.jboss.jandex.Type> expectedTypes = expectedTypes(method);
        return allObservers.stream()
                .filter(observer -> {
                    org.jboss.jandex.Type observedType = observer.getObservedType();
                    if (observedType.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE) {
                        observedType = org.jboss.jandex.PrimitiveType.box(observedType.asPrimitiveType());
                    }
                    List<org.jboss.jandex.Type> types = JandexTypeSystem.of(index).typeWithSuperTypes(observedType, false);
                    Set<org.jboss.jandex.Type> rawTypes = types.stream()
                            .map(it -> org.jboss.jandex.ClassType.create(it.name()))
                            .collect(Collectors.toSet());
                    for (org.jboss.jandex.Type expectedType : expectedTypes) {
                        if (types.contains(expectedType)) {
                            return true;
                        }

                        if (expectedType.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE) {
                            expectedType = org.jboss.jandex.PrimitiveType.box(expectedType.asPrimitiveType());
                        }
                        if (expectedType.kind() == org.jboss.jandex.Type.Kind.CLASS && rawTypes.contains(expectedType)) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(it -> (ObserverInfo) new ObserverInfoImpl(index, annotationOverlay, it))
                .toList();
    }

    @Override
    Object argumentForExtensionMethod(ExtensionMethodParameter type, ExtensionMethod method) {
        return switch (type) {
            case INVOKER_FACTORY -> new InvokerFactoryImpl(invokerFactory);
            case TYPES -> new TypesImpl(index, annotationOverlay);
            default -> super.argumentForExtensionMethod(type, method);
        };
    }
}
