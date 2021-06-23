package io.quarkus.spring.security.deployment;

import static io.quarkus.security.deployment.SecurityTransformerUtils.findFirstStandardSecurityAnnotation;
import static io.quarkus.security.deployment.SecurityTransformerUtils.hasStandardSecurityAnnotation;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.security.deployment.AdditionalSecurityCheckBuildItem;
import io.quarkus.security.deployment.SecurityTransformerUtils;
import io.quarkus.security.runtime.SecurityCheckRecorder;
import io.quarkus.security.runtime.interceptor.check.SecurityCheck;
import io.quarkus.spring.di.deployment.SpringBeanNameToDotNameBuildItem;
import io.quarkus.spring.security.runtime.interceptor.SpringPreauthorizeInterceptor;
import io.quarkus.spring.security.runtime.interceptor.SpringSecuredInterceptor;
import io.quarkus.spring.security.runtime.interceptor.SpringSecurityRecorder;
import io.quarkus.spring.security.runtime.interceptor.check.PrincipalNameFromParameterSecurityCheck;

class SpringSecurityProcessor {

    private static final String PARAMETER_EQ_PRINCIPAL_USERNAME_REGEX = "#(\\w+)(\\.(\\w+))?\\s+[=!]=\\s+(authentication.)?principal.username";
    private static final Pattern PARAMETER_EQ_PRINCIPAL_USERNAME_PATTERN = Pattern
            .compile(PARAMETER_EQ_PRINCIPAL_USERNAME_REGEX);

    private static final int PARAMETER_EQ_PRINCIPAL_USERNAME_PARAMETER_NAME_GROUP = 1;
    private static final int PARAMETER_EQ_PRINCIPAL_USERNAME_PROPERTY_ACCESSOR_MATCHER_GROUP = 3;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SPRING_SECURITY);
    }

    @BuildStep
    void registerSecurityInterceptors(BuildProducer<InterceptorBindingRegistrarBuildItem> registrars,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        registrars.produce(new InterceptorBindingRegistrarBuildItem(new SpringSecurityAnnotationsRegistrar()));
        beans.produce(new AdditionalBeanBuildItem(SpringSecuredInterceptor.class));
        beans.produce(new AdditionalBeanBuildItem(SpringPreauthorizeInterceptor.class));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void addSpringSecuredSecurityCheck(CombinedIndexBuildItem index,
            SecurityCheckRecorder securityCheckRecorder,
            BuildProducer<AdditionalSecurityCheckBuildItem> additionalSecurityCheckBuildItems) {

        Set<MethodInfo> methodsWithSecurityAnnotation = new HashSet<>();

        // first first go through the list of annotated methods
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
                        securityCheckRecorder.rolesAllowed(rolesAllowed)));
                methodsWithSecurityAnnotation.add(methodInfo);
            }
        }

        // now check for instances on classes and methods that aren't already annotated with a security annotation
        for (AnnotationInstance instance : index.getIndex().getAnnotations(DotNames.SPRING_SECURED)) {
            if (instance.value() == null) {
                continue;
            }
            String[] rolesAllowed = instance.value().asStringArray();

            if (instance.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo classInfo = instance.target().asClass();
                checksStandardSecurity(instance, classInfo);
                for (MethodInfo methodInfo : classInfo.methods()) {
                    if (!isPublicNonStaticNonConstructor(methodInfo)) {
                        continue;
                    }
                    checksStandardSecurity(instance, methodInfo);
                    if (hasSpringSecurityAnnotationOtherThan(methodInfo, DotNames.SPRING_SECURED)) {
                        continue;
                    }
                    if (!methodsWithSecurityAnnotation.contains(methodInfo)) {
                        additionalSecurityCheckBuildItems.produce(new AdditionalSecurityCheckBuildItem(methodInfo,
                                securityCheckRecorder.rolesAllowed(rolesAllowed)));
                    }
                }
            }
        }
    }

    private boolean hasSpringSecurityAnnotationOtherThan(MethodInfo methodInfo, DotName excluded) {
        Set<DotName> toCheck = new HashSet<>(DotNames.SUPPORTED_SPRING_SECURITY_ANNOTATIONS);
        toCheck.remove(excluded);
        List<AnnotationInstance> annotations = methodInfo.annotations();
        for (AnnotationInstance instance : annotations) {
            if (toCheck.contains(instance.name())) {
                return true;
            }
        }
        return false;
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

    @BuildStep
    void locatePreAuthorizedInstances(
            CombinedIndexBuildItem index,
            BuildProducer<SpringPreAuthorizeAnnotatedMethodBuildItem> springPreAuthorizeAnnotatedMethods,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        Map<MethodInfo, AnnotationInstance> result = new HashMap<>();

        // first first go through the list of annotated methods
        for (AnnotationInstance instance : index.getIndex().getAnnotations(DotNames.SPRING_PRE_AUTHORIZE)) {
            if (instance.value() == null) {
                continue;
            }
            if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            MethodInfo methodInfo = instance.target().asMethod();
            checksStandardSecurity(instance, methodInfo);
            result.put(methodInfo, instance);
        }

        // don't use ClassInfo in a Set because it (purposely) doesn't implement equals and hashcode
        Map<DotName, ClassInfo> metaAnnotations = new HashMap<>();

        // now check for instances on classes and methods that aren't already annotated with a security annotation
        for (AnnotationInstance instance : index.getIndex().getAnnotations(DotNames.SPRING_PRE_AUTHORIZE)) {
            if (instance.value() == null) {
                continue;
            }
            if (instance.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo classInfo = instance.target().asClass();
            if (classInfo.isAnnotation()) {
                // if the instance is an annotation we need to record it and handle it later
                metaAnnotations.put(classInfo.name(), classInfo);
                continue;
            }
            checksStandardSecurity(instance, classInfo);
            for (MethodInfo methodInfo : classInfo.methods()) {
                if (!isPublicNonStaticNonConstructor(methodInfo)) {
                    continue;
                }
                checksStandardSecurity(instance, methodInfo);
                if (hasSpringSecurityAnnotationOtherThan(methodInfo, DotNames.SPRING_PRE_AUTHORIZE)) {
                    continue;
                }
                if (!result.containsKey(methodInfo)) {
                    result.put(methodInfo, instance);
                }
            }
        }

        /*
         * For each meta-annotation add the value of @PreAuthorize to the method tracking map
         */
        Set<DotName> classesInNeedOfAnnotationTransformation = new HashSet<>();
        for (ClassInfo metaAnnotation : metaAnnotations.values()) {
            for (AnnotationInstance instance : index.getIndex().getAnnotations(metaAnnotation.name())) {
                if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                    continue;
                }
                MethodInfo methodInfo = instance.target().asMethod();
                checksStandardSecurity(instance, methodInfo);
                result.put(methodInfo, metaAnnotation.classAnnotation(DotNames.SPRING_PRE_AUTHORIZE));
                classesInNeedOfAnnotationTransformation.add(methodInfo.declaringClass().name());
            }
        }

        springPreAuthorizeAnnotatedMethods.produce(new SpringPreAuthorizeAnnotatedMethodBuildItem(result));

        /*
         * Go through each of the classes that have an instance of a meta-annotation and add the @PreAuthorize
         * annotation to the class.
         * This is done in order so Arc will ensure that an interceptor will be introduced
         */
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext transformationContext) {
                ClassInfo classInfo = transformationContext.getTarget().asClass();
                if (classesInNeedOfAnnotationTransformation.contains(classInfo.name())) {
                    transformationContext.transform()
                            .add(DotNames.SPRING_PRE_AUTHORIZE, AnnotationValue.createStringValue("value", "")).done();
                }
            }
        }));
    }

    /**
     * The generation needs to be done in it's own build step otherwise we can end up with build cycle errors
     */
    @BuildStep
    void generateNecessarySupportClasses(CombinedIndexBuildItem index,
            SpringPreAuthorizeAnnotatedMethodBuildItem springPreAuthorizeAnnotatedMethods,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {

        Map<DotName, Set<FieldInfo>> stringPropertiesInNeedOfGeneratedAccessors = new HashMap<>();

        for (Map.Entry<MethodInfo, AnnotationInstance> entry : springPreAuthorizeAnnotatedMethods.getMethodToInstanceMap()
                .entrySet()) {
            AnnotationInstance instance = entry.getValue();
            MethodInfo methodInfo = entry.getKey();
            String value = instance.value().asString().trim();

            String[] parts = { value };
            if (value.toLowerCase().contains(" and ")) {
                parts = value.split("(?i) and ");
            } else if (value.toLowerCase().contains(" or ")) {
                parts = value.split("(?i) or ");
            }

            /*
             * this is essentially the same loop as in addSpringPreAuthorizeSecurityCheck but only deals with cases where
             * where beans need to be generated
             */
            for (String part : parts) {
                part = part.trim();
                if (part.matches(PARAMETER_EQ_PRINCIPAL_USERNAME_REGEX)) {
                    Matcher matcher = PARAMETER_EQ_PRINCIPAL_USERNAME_PATTERN.matcher(part);
                    if (!matcher.find()) { // should never happen
                        throw SpringSecurityProcessorUtil.createGenericMalformedException(methodInfo, part);
                    }

                    ParameterNameAndIndex parameterNameAndIndex = getParameterNameAndIndexForPrincipalUserNameReference(
                            methodInfo,
                            matcher, part);

                    String propertyName = matcher.group(PARAMETER_EQ_PRINCIPAL_USERNAME_PROPERTY_ACCESSOR_MATCHER_GROUP);
                    if (propertyName != null) {
                        /*
                         * In this we need to call a getter method on the parameter. In order to do that we need to generate
                         * an accessor for that method (which is ensured to return type String since that is the type of the
                         * username).
                         */
                        StringPropertyAccessorData stringPropertyAccessorData = StringPropertyAccessorData.from(
                                methodInfo, parameterNameAndIndex.getIndex(),
                                propertyName, index.getIndex(),
                                part);

                        Set<FieldInfo> fields = stringPropertiesInNeedOfGeneratedAccessors.getOrDefault(
                                stringPropertyAccessorData.getMatchingParameterClassInfo().name(),
                                new HashSet<>());
                        fields.add(stringPropertyAccessorData.getMatchingParameterFieldInfo());
                        stringPropertiesInNeedOfGeneratedAccessors.put(
                                stringPropertyAccessorData.getMatchingParameterClassInfo().name(),
                                fields);
                    }
                }
            }
        }

        // actually generate the accessor classes as beans
        if (!stringPropertiesInNeedOfGeneratedAccessors.isEmpty()) {
            GeneratedBeanGizmoAdaptor classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
            Set<String> generatedBeanClassNames = new HashSet<>(stringPropertiesInNeedOfGeneratedAccessors.keySet().size());
            for (Map.Entry<DotName, Set<FieldInfo>> entry : stringPropertiesInNeedOfGeneratedAccessors.entrySet()) {
                String generateClassName = StringPropertyAccessorGenerator.generate(entry.getKey(), entry.getValue(),
                        classOutput);
                generatedBeanClassNames.add(generateClassName);
            }
            unremovableBeans.produce((new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNamesExclusion(generatedBeanClassNames))));
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void addSpringPreAuthorizeSecurityCheck(CombinedIndexBuildItem index,
            SecurityCheckRecorder securityCheckRecorder,
            SpringSecurityRecorder springSecurityRecorder,
            SpringPreAuthorizeAnnotatedMethodBuildItem springPreAuthorizeAnnotatedMethods,
            SpringBeanNameToDotNameBuildItem springBeanNames,
            BuildProducer<AdditionalSecurityCheckBuildItem> additionalSecurityChecks,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        Map<String, DotName> springBeansNameToDotName = springBeanNames.getMap();
        Map<String, ClassInfo> springBeansNameToClassInfo = new HashMap<>();
        Set<String> beansReferencedInPreAuthorized = new HashSet<>();
        BeanMethodInvocationGenerator beanMethodInvocationGenerator = new BeanMethodInvocationGenerator(index.getIndex(),
                springBeansNameToDotName, springBeansNameToClassInfo, beansReferencedInPreAuthorized,
                new GeneratedClassGizmoAdaptor(generatedClasses, true));

        for (Map.Entry<MethodInfo, AnnotationInstance> entry : springPreAuthorizeAnnotatedMethods.getMethodToInstanceMap()
                .entrySet()) {
            AnnotationInstance instance = entry.getValue();

            MethodInfo methodInfo = entry.getKey();
            String value = instance.value().asString().trim();

            /*
             * TODO: this serves fine for most purposes but a full blown solution will need a proper parser
             */

            boolean containsAnd = false;
            boolean containsOr = false;
            String lowercaseValue = value.toLowerCase();
            if (lowercaseValue.contains(" or ")) {
                containsOr = true;
            }
            if (lowercaseValue.contains(" and ")) {
                containsAnd = true;
            }

            if (containsAnd && containsOr) {
                throw new IllegalStateException(
                        "Currently expressions containing both logical 'and' / 'or' are not supported. Offending expression is "
                                + value + "' in the @PreAuthorize annotation on method '" + methodInfo.name()
                                + "' of class '" + methodInfo.declaringClass());
            }

            String[] parts = { value };
            if (containsAnd) {
                parts = value.split("(?i) and ");
            } else if (containsOr) {
                parts = value.split("(?i) or ");
            }

            List<SecurityCheck> securityChecks = new ArrayList<>(parts.length);

            for (String part : parts) {
                part = part.trim();
                if (part.equals("permitAll()")) {
                    securityChecks.add(securityCheckRecorder.permitAll());
                } else if (part.equals("denyAll()")) {
                    securityChecks.add(securityCheckRecorder.denyAll());
                } else if (part.equals("isAnonymous()")) {
                    securityChecks.add(springSecurityRecorder.anonymous());
                } else if (part.replaceAll("\\s", "").equals("isAuthenticated()")) {

                    securityChecks.add(securityCheckRecorder.authenticated());

                } else if (part.startsWith("hasRole(")) {

                    String hasRoleValue = part.replace("hasRole(", "").replace(")", "");
                    Supplier<String[]> hasRoleValueProducer = HasRoleValueUtil.getHasRoleValueProducer(hasRoleValue,
                            methodInfo,
                            index.getIndex(), springBeansNameToDotName, springBeansNameToClassInfo,
                            beansReferencedInPreAuthorized,
                            springSecurityRecorder);
                    securityChecks.add(
                            springSecurityRecorder.rolesAllowed(Collections.singletonList(hasRoleValueProducer)));

                } else if (part.startsWith("hasAnyRole(")) {

                    String hasRoleValues = part.replace("hasAnyRole(", "").replace(")", "");
                    String[] hasRoleParts = hasRoleValues.split(",");
                    List<Supplier<String[]>> hasRoleValueProducers = new ArrayList<>(hasRoleParts.length);
                    for (String hasRolePart : hasRoleParts) {
                        hasRoleValueProducers
                                .add(HasRoleValueUtil.getHasRoleValueProducer(hasRolePart.trim(), methodInfo, index.getIndex(),
                                        springBeansNameToDotName,
                                        springBeansNameToClassInfo, beansReferencedInPreAuthorized, springSecurityRecorder));
                    }
                    securityChecks.add(springSecurityRecorder.rolesAllowed(hasRoleValueProducers));

                } else if (part.matches(PARAMETER_EQ_PRINCIPAL_USERNAME_REGEX)) { // TODO this section needs to be improved

                    Matcher matcher = PARAMETER_EQ_PRINCIPAL_USERNAME_PATTERN.matcher(part);
                    if (!matcher.find()) { // should never happen
                        throw SpringSecurityProcessorUtil.createGenericMalformedException(methodInfo, part);
                    }
                    ParameterNameAndIndex parameterNameAndIndex = getParameterNameAndIndexForPrincipalUserNameReference(
                            methodInfo,
                            matcher, part);

                    String propertyName = matcher.group(PARAMETER_EQ_PRINCIPAL_USERNAME_PROPERTY_ACCESSOR_MATCHER_GROUP);
                    if (propertyName == null) { // this is the case where the parameter is supposed to be a string
                        if (!DotNames.STRING.equals(methodInfo.parameters().get(parameterNameAndIndex.getIndex()).name())) {
                            throw new IllegalArgumentException(
                                    "Expression: '" + part + "' in the @PreAuthorize annotation on method '" + methodInfo.name()
                                            + "' of class '" + methodInfo.declaringClass() + "' references method parameter '"
                                            + parameterNameAndIndex.getName() + "' which is not a string");
                        }
                        PrincipalNameFromParameterSecurityCheck.CheckType checkType = part.contains("==")
                                ? PrincipalNameFromParameterSecurityCheck.CheckType.EQ
                                : PrincipalNameFromParameterSecurityCheck.CheckType.NEQ;
                        securityChecks.add(springSecurityRecorder
                                .principalNameFromParameterSecurityCheck(parameterNameAndIndex.getIndex(), checkType));
                    } else {
                        /*
                         * In this the security check needs to check against a property of the method parameter.
                         * We use a special SecurityCheck that leverages a generated accessor class
                         * (see the generateStringPropertyAccessors method)
                         */
                        StringPropertyAccessorData stringPropertyAccessorData = StringPropertyAccessorData.from(
                                methodInfo, parameterNameAndIndex.getIndex(),
                                propertyName, index.getIndex(),
                                part);

                        securityChecks.add(springSecurityRecorder.principalNameFromParameterObjectSecurityCheck(
                                parameterNameAndIndex.getIndex(),
                                stringPropertyAccessorData.getMatchingParameterClassInfo().name().toString(),
                                StringPropertyAccessorGenerator
                                        .getAccessorClassName(
                                                stringPropertyAccessorData.getMatchingParameterClassInfo().name()),
                                stringPropertyAccessorData.getMatchingParameterFieldInfo().name()));

                    }
                } else if (part.matches(SpringSecurityProcessorUtil.BASIC_BEAN_METHOD_INVOCATION_REGEX)) {
                    String generatedClassName = beanMethodInvocationGenerator.generateSecurityCheck(part, methodInfo);
                    securityChecks.add(springSecurityRecorder.fromGeneratedClass(generatedClassName));
                } else {
                    throw SpringSecurityProcessorUtil.createGenericMalformedException(methodInfo, part);
                }
            }

            // if there is only one security check we don't need to do any delegation
            if (securityChecks.size() == 1) {
                additionalSecurityChecks.produce(new AdditionalSecurityCheckBuildItem(methodInfo, securityChecks.get(0)));
            } else {
                if (containsAnd) {
                    additionalSecurityChecks.produce(new AdditionalSecurityCheckBuildItem(methodInfo,
                            springSecurityRecorder.allDelegating(securityChecks)));
                } else if (containsOr) {
                    additionalSecurityChecks.produce(new AdditionalSecurityCheckBuildItem(methodInfo,
                            springSecurityRecorder.anyDelegating(securityChecks)));
                }
            }
        }

        if (!beansReferencedInPreAuthorized.isEmpty()) {
            unremovableBeans.produce((new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNamesExclusion(beansReferencedInPreAuthorized))));
        }
    }

    private ParameterNameAndIndex getParameterNameAndIndexForPrincipalUserNameReference(MethodInfo methodInfo, Matcher matcher,
            String expression) {
        String parameterName = matcher.group(PARAMETER_EQ_PRINCIPAL_USERNAME_PARAMETER_NAME_GROUP);

        return new ParameterNameAndIndex(SpringSecurityProcessorUtil.getParameterIndex(methodInfo, parameterName, expression),
                parameterName);
    }

}
