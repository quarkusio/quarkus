package io.quarkus.spring.security.deployment;

import static io.quarkus.security.deployment.SecurityTransformerUtils.findFirstStandardSecurityAnnotation;
import static io.quarkus.security.deployment.SecurityTransformerUtils.hasStandardSecurityAnnotation;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.security.deployment.AdditionalSecurityCheckBuildItem;
import io.quarkus.security.deployment.SecurityCheckInstantiationUtil;
import io.quarkus.security.deployment.SecurityTransformerUtils;
import io.quarkus.spring.security.runtime.interceptor.SpringSecuredInterceptor;

class SpringSecurityProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SPRING_SECURITY);
    }

    @BuildStep
    void registerSecurityInterceptors(BuildProducer<InterceptorBindingRegistrarBuildItem> registrars,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        registrars.produce(new InterceptorBindingRegistrarBuildItem(new SpringSecurityAnnotationsRegistrar()));
        beans.produce(new AdditionalBeanBuildItem(SpringSecuredInterceptor.class));
    }

    @BuildStep
    void addSpringSecuredSecurityCheck(ApplicationIndexBuildItem index,
            BuildProducer<AdditionalSecurityCheckBuildItem> additionalSecurityCheckBuildItems) {

        Set<MethodInfo> methodsWithSecurityAnnotation = new HashSet<>();
        for (AnnotationInstance instance : index.getIndex().getAnnotations(DotNames.SPRING_SECURED)) {
            if (instance.value() == null) {
                continue;
            }
            String[] rolesAllowed = instance.value().asStringArray();

            if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
                MethodInfo methodInfo = instance.target().asMethod();
                checksStandardSecurity(instance, methodInfo);
                checksStandardSecurity(instance, methodInfo.declaringClass());
                additionalSecurityCheckBuildItems.produce(new AdditionalSecurityCheckBuildItem(methodInfo,
                        SecurityCheckInstantiationUtil.rolesAllowedSecurityCheck(rolesAllowed)));
                methodsWithSecurityAnnotation.add(methodInfo);
            } else if (instance.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo classInfo = instance.target().asClass();
                checksStandardSecurity(instance, classInfo);
                for (MethodInfo methodInfo : classInfo.methods()) {
                    if (!isPublicNonStaticNonConstructor(methodInfo)) {
                        continue;
                    }
                    checksStandardSecurity(instance, methodInfo);
                    if (!methodsWithSecurityAnnotation.contains(methodInfo)) {
                        additionalSecurityCheckBuildItems.produce(new AdditionalSecurityCheckBuildItem(methodInfo,
                                SecurityCheckInstantiationUtil.rolesAllowedSecurityCheck(rolesAllowed)));
                    }
                }
            }
        }
    }

    //Validates that there is no @Secured with the standard security annotations at class level
    private void checksStandardSecurity(AnnotationInstance instance, ClassInfo classInfo) {
        if (hasStandardSecurityAnnotation(classInfo)) {
            Optional<AnnotationInstance> firstStandardSecurityAnnotation = findFirstStandardSecurityAnnotation(classInfo);
            if (firstStandardSecurityAnnotation.isPresent()) {
                String securityAnnotationName = findFirstStandardSecurityAnnotation(classInfo).get().name()
                        .withoutPackagePrefix();
                throw new IllegalArgumentException("An invalid security annotation combination was detected: Found @"
                        + instance.name().withoutPackagePrefix() + " and @" + securityAnnotationName + " on class "
                        + classInfo.simpleName());
            }
        }
    }

    //Validates that there is no @Secured with the standard security annotations at method level
    private void checksStandardSecurity(AnnotationInstance instance, MethodInfo methodInfo) {
        if (SecurityTransformerUtils.hasStandardSecurityAnnotation(methodInfo)) {
            Optional<AnnotationInstance> firstStandardSecurityAnnotation = SecurityTransformerUtils
                    .findFirstStandardSecurityAnnotation(methodInfo);
            if (firstStandardSecurityAnnotation.isPresent()) {
                String securityAnnotationName = SecurityTransformerUtils.findFirstStandardSecurityAnnotation(methodInfo).get()
                        .name()
                        .withoutPackagePrefix();
                throw new IllegalArgumentException("An invalid security annotation combination was detected: Found "
                        + instance.name().withoutPackagePrefix() + " and " + securityAnnotationName + " on method "
                        + methodInfo.name());
            }
        }
    }

    private boolean isPublicNonStaticNonConstructor(MethodInfo methodInfo) {
        return Modifier.isPublic(methodInfo.flags()) && !Modifier.isStatic(methodInfo.flags())
                && !"<init>".equals(methodInfo.name());
    }
}
