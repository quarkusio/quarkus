package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.runtime.ConfigBeanCreator;
import io.quarkus.arc.runtime.ConfigDeploymentTemplate;
import io.quarkus.arc.runtime.QuarkusConfigProducer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

/**
 * MicroProfile Config related build steps.
 */
public class ConfigBuildStep {

    private static final DotName CONFIG_PROPERTY_NAME = DotName.createSimple(ConfigProperty.class.getName());
    private static final DotName SET_NAME = DotName.createSimple(Set.class.getName());
    private static final DotName LIST_NAME = DotName.createSimple(List.class.getName());

    @BuildStep
    AdditionalBeanBuildItem bean() {
        return new AdditionalBeanBuildItem(QuarkusConfigProducer.class);
    }

    @BuildStep
    BeanRegistrarBuildItem analyzeConfigPropertyInjectionPoints(BuildProducer<ConfigPropertyBuildItem> configProperties,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        return new BeanRegistrarBuildItem(new BeanRegistrar() {

            @Override
            public void register(RegistrationContext context) {
                Set<Type> customBeanTypes = new HashSet<>();

                for (InjectionPointInfo injectionPoint : context.get(Key.INJECTION_POINTS)) {
                    if (injectionPoint.hasDefaultedQualifier()) {
                        // Defaulted qualifier means no @ConfigProperty
                        continue;
                    }

                    AnnotationInstance configProperty = injectionPoint.getRequiredQualifier(CONFIG_PROPERTY_NAME);
                    if (configProperty != null) {
                        AnnotationValue nameValue = configProperty.value("name");
                        AnnotationValue defaultValue = configProperty.value("defaultValue");
                        String propertyName;
                        if (nameValue != null) {
                            propertyName = nameValue.asString();
                        } else {
                            // org.acme.Foo.config
                            if (injectionPoint.isField()) {
                                FieldInfo field = injectionPoint.getTarget().asField();
                                propertyName = getPropertyName(field.name(), field.declaringClass());
                            } else if (injectionPoint.isParam()) {
                                MethodInfo method = injectionPoint.getTarget().asMethod();
                                propertyName = getPropertyName(method.parameterName(injectionPoint.getPosition()),
                                        method.declaringClass());
                            } else {
                                throw new IllegalStateException("Unsupported injection point target: " + injectionPoint);
                            }
                        }

                        // Register a custom bean for injection points that are not handled by ConfigProducer
                        Type requiredType = injectionPoint.getRequiredType();
                        if (!isHandledByProducers(requiredType)) {
                            customBeanTypes.add(requiredType);
                        }

                        if (DotNames.OPTIONAL.equals(requiredType.name())) {
                            // Never validate Optional values
                            continue;
                        }
                        if (defaultValue != null && !ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue.asString())) {
                            // No need to validate properties with default values
                            continue;
                        }
                        String propertyType = requiredType.name().toString();
                        if (requiredType.kind() != Kind.ARRAY && requiredType.kind() != Kind.PRIMITIVE) {
                            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, propertyType));
                        }
                        configProperties.produce(new ConfigPropertyBuildItem(propertyName, propertyType));
                    }
                }

                for (Type type : customBeanTypes) {
                    if (type.kind() != Kind.ARRAY) {
                        // Implicit converters are most likely used
                        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, type.name().toString()));
                    }
                    context.configure(
                            type.kind() == Kind.ARRAY ? DotName.createSimple(ConfigBeanCreator.class.getName()) : type.name())
                            .creator(ConfigBeanCreator.class)
                            .providerType(type)
                            .types(type)
                            .qualifiers(AnnotationInstance.create(CONFIG_PROPERTY_NAME, null, Collections.emptyList()))
                            .param("requiredType", type.name().toString()).done();
                }
            }
        });
    }

    @BuildStep
    AutoInjectAnnotationBuildItem autoInjectConfigProperty() {
        return new AutoInjectAnnotationBuildItem(CONFIG_PROPERTY_NAME);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void validateConfigProperties(ConfigDeploymentTemplate template, List<ConfigPropertyBuildItem> configProperties,
            BeanContainerBuildItem beanContainer) {
        // IMPL NOTE: we do depend on BeanContainerBuildItem to make sure that the BeanDeploymentValidator finished its processing

        Map<String, Set<String>> propNamesToClasses = configProperties.stream().collect(
                groupingBy(ConfigPropertyBuildItem::getPropertyName,
                        mapping(ConfigPropertyBuildItem::getPropertyType, toSet())));
        template.validateConfigProperties(propNamesToClasses);
    }

    private String getPropertyName(String name, ClassInfo declaringClass) {
        StringBuilder builder = new StringBuilder();
        if (declaringClass.enclosingClass() == null) {
            builder.append(declaringClass.name());
        } else {
            builder.append(declaringClass.enclosingClass()).append(".").append(declaringClass.simpleName());
        }
        return builder.append(".").append(name).toString();
    }

    private boolean isHandledByProducers(Type type) {
        if (type.kind() == Kind.ARRAY) {
            return false;
        }
        if (type.kind() == Kind.PRIMITIVE) {
            switch (type.asPrimitiveType().primitive()) {
                case BOOLEAN:
                case DOUBLE:
                case FLOAT:
                case LONG:
                case INT:
                    return true;
                default:
                    return false;
            }
        }
        return DotNames.STRING.equals(type.name()) || DotNames.OPTIONAL.equals(type.name()) || SET_NAME.equals(type.name())
                || LIST_NAME.equals(type.name()) || DotNames.LONG.equals(type.name()) || DotNames.FLOAT.equals(type.name())
                || DotNames.INTEGER.equals(type.name()) || DotNames.BOOLEAN.equals(type.name())
                || DotNames.DOUBLE.equals(type.name());
    }

}
