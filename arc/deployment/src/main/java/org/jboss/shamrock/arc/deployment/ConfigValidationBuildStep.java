package org.jboss.shamrock.arc.deployment;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.protean.arc.processor.BeanDeploymentValidator;
import org.jboss.protean.arc.processor.DotNames;
import org.jboss.protean.arc.processor.InjectionPointInfo;
import org.jboss.shamrock.annotations.BuildStep;

/**
 * TODO: we should move this to the core-deployment module
 * 
 * @author Martin Kouba
 */
public class ConfigValidationBuildStep {
    
    @BuildStep
    BeanDeploymentValidatorBuildItem beanDeploymentValidator() {
        return new BeanDeploymentValidatorBuildItem(new ConfigBeanDeploymentValidator());
    }
    
    static class ConfigBeanDeploymentValidator implements BeanDeploymentValidator {

        private static final DotName CONFIG_PROPERTY_NAME = DotName.createSimple(ConfigProperty.class.getName());

        @Override
        public void validate(ValidationContext validationContext) {
            
            Config config = ConfigProvider.getConfig();
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            
            for (InjectionPointInfo injectionPoint : validationContext.get(Key.INJECTION_POINTS)) {
                if (injectionPoint.hasDefaultedQualifier()) {
                    continue;
                }
                AnnotationInstance configProperty = injectionPoint.getRequiredQualifiers()
                        .stream()
                        .filter(a -> a.name()
                                .equals(CONFIG_PROPERTY_NAME))
                        .findFirst()
                        .orElse(null);
                if (configProperty != null) {
                    if (DotNames.OPTIONAL.equals(injectionPoint.getRequiredType().name())) {
                        continue;
                    }
                    String key = configProperty.value("name").asString();
                    // TODO: collection types
                    Class<?> type;
                    try {
                        type = tccl.loadClass(injectionPoint.getRequiredType().name().toString());
                        if (!config.getOptionalValue(key, type).isPresent()) {
                            AnnotationValue defaultValue = configProperty.value("defaultValue");
                            if (defaultValue == null || ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue.asString())) {
                                validationContext.addDeploymentProblem(new IllegalStateException("No config value exists for: " + key));
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Unable to verify config injection point: " + injectionPoint, e);
                    }
                }
            }
        }

    }

}
