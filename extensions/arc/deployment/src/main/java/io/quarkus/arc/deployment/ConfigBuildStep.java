package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.runtime.ConfigDeploymentTemplate;
import io.quarkus.arc.runtime.QuarkusConfigProducer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;

/**
 * MicroProfile Config related build steps.
 */
public class ConfigBuildStep {

    private static final DotName CONFIG_PROPERTY_NAME = DotName.createSimple(ConfigProperty.class.getName());

    @BuildStep
    AdditionalBeanBuildItem bean() {
        return new AdditionalBeanBuildItem(QuarkusConfigProducer.class);
    }

    @BuildStep
    BeanDeploymentValidatorBuildItem collectMandatoryConfigProperties(BuildProducer<ConfigPropertyBuildItem> configProperties) {

        return new BeanDeploymentValidatorBuildItem(new BeanDeploymentValidator() {

            @Override
            public void validate(ValidationContext validationContext) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();

                for (InjectionPointInfo injectionPoint : validationContext.get(Key.INJECTION_POINTS)) {
                    if (injectionPoint.hasDefaultedQualifier()) {
                        // Defaulted qualifier means no @ConfigProperty
                        continue;
                    }
                    if (DotNames.OPTIONAL.equals(injectionPoint.getRequiredType().name())) {
                        // Never validate Optional values
                        continue;
                    }
                    AnnotationInstance configProperty = injectionPoint.getRequiredQualifiers().stream()
                            .filter(a -> a.name().equals(CONFIG_PROPERTY_NAME))
                            .findFirst().orElse(null);
                    if (configProperty != null) {
                        AnnotationValue defaultValue = configProperty.value("defaultValue");
                        if (defaultValue != null && !ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue.asString())) {
                            // No need to validate properties with default values
                            continue;
                        }
                        AnnotationValue nameValue = configProperty.value("name");
                        String propertyName;
                        if (nameValue != null) {
                            propertyName = nameValue.asString();
                        } else {
                            // org.acme.Foo.config
                            if (injectionPoint.isField()) {
                                FieldInfo field = injectionPoint.getTarget().asField();
                                propertyName = (field.declaringClass().enclosingClass() == null ? field.declaringClass().name()
                                        : field.declaringClass().enclosingClass() + "." + field.declaringClass().simpleName())
                                        + "." + field.name();
                            } else if (injectionPoint.isParam()) {
                                MethodInfo method = injectionPoint.getTarget().asMethod();
                                propertyName = (method.declaringClass().enclosingClass() == null
                                        ? method.declaringClass().name()
                                        : method.declaringClass().enclosingClass() + "." + method.declaringClass().simpleName())
                                        + "."
                                        + method.parameterName(injectionPoint.getPosition());
                            } else {
                                throw new IllegalStateException("Unsupported injection point target: " + injectionPoint);
                            }
                        }
                        Class<?> propertyType;

                        if (injectionPoint.getRequiredType().kind() == Type.Kind.PRIMITIVE) {
                            switch (injectionPoint.getRequiredType().asPrimitiveType().primitive()) {
                                case BOOLEAN:
                                    propertyType = Boolean.TYPE;
                                    break;
                                case BYTE:
                                    propertyType = Byte.TYPE;
                                    break;
                                case CHAR:
                                    propertyType = Character.TYPE;
                                    break;
                                case DOUBLE:
                                    propertyType = Double.TYPE;
                                    break;
                                case INT:
                                    propertyType = Integer.TYPE;
                                    break;
                                case FLOAT:
                                    propertyType = Float.TYPE;
                                    break;
                                case LONG:
                                    propertyType = Long.TYPE;
                                    break;
                                case SHORT:
                                    propertyType = Short.TYPE;
                                    break;
                                default:
                                    throw new IllegalArgumentException(
                                            "Not a supported primitive type: "
                                                    + injectionPoint.getRequiredType().asPrimitiveType().primitive());
                            }
                        } else {
                            try {
                                propertyType = tccl.loadClass(injectionPoint.getRequiredType().name().toString());
                            } catch (ClassNotFoundException e) {
                                throw new IllegalStateException("Unable to load the config property type: " + injectionPoint,
                                        e);
                            }
                        }
                        configProperties.produce(new ConfigPropertyBuildItem(propertyName, propertyType));
                    }
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

        Map<String, Set<Class<?>>> propNamesToClasses = configProperties.stream().collect(
                groupingBy(ConfigPropertyBuildItem::getPropertyName,
                        mapping(ConfigPropertyBuildItem::getPropertyType, toSet())));
        template.validateConfigProperties(propNamesToClasses);
    }

}
