package io.quarkus.spring.security.deployment;

import static io.quarkus.gizmo.FieldDescriptor.of;
import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.spring.security.deployment.SpringSecurityProcessorUtil.BASIC_BEAN_METHOD_INVOCATION_PATTERN;
import static io.quarkus.spring.security.deployment.SpringSecurityProcessorUtil.createGenericMalformedException;
import static io.quarkus.spring.security.deployment.SpringSecurityProcessorUtil.getClassInfoFromBeanName;
import static io.quarkus.spring.security.deployment.SpringSecurityProcessorUtil.getParameterIndex;

import java.lang.reflect.Modifier;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.spring.security.runtime.interceptor.check.AbstractBeanMethodSecurityCheck;

class BeanMethodInvocationGenerator {

    private static final String METHOD_PARAMETER_REGEX = "#(\\w+)";
    private static final Pattern METHOD_PARAMETER_PATTERN = Pattern.compile(METHOD_PARAMETER_REGEX);

    private final IndexView index;
    private final Map<String, DotName> springBeansNameToDotName;
    private final Map<String, ClassInfo> springBeansNameToClassInfo;
    private final Set<String> beansReferencedInPreAuthorized;
    private final ClassOutput classOutput;

    private final Map<String, String> alreadyGeneratedClasses = new HashMap<>();

    public BeanMethodInvocationGenerator(IndexView index, Map<String, DotName> springBeansNameToDotName,
            Map<String, ClassInfo> springBeansNameToClassInfo, Set<String> beansReferencedInPreAuthorized,
            ClassOutput classOutput) {
        this.index = index;
        this.springBeansNameToDotName = springBeansNameToDotName;
        this.springBeansNameToClassInfo = springBeansNameToClassInfo;
        this.beansReferencedInPreAuthorized = beansReferencedInPreAuthorized;
        this.classOutput = classOutput;
    }

    /**
     * Returns the name of the generated class that implements the security check
     * The generated class is an implementation of {@link AbstractBeanMethodSecurityCheck}
     * that simply calls the proper bean method with the correct arguments
     */
    final String generateSecurityCheck(String expression, MethodInfo securedMethodInfo) {
        String paramTypesDescriptor = getParamTypesDescriptor(securedMethodInfo);
        int parametersStartIndex = expression.indexOf('(');
        int parametersEndIndex = expression.indexOf(')');
        String[] beanMethodArgumentExpressions = {};
        if (parametersEndIndex > parametersStartIndex + 1) {
            beanMethodArgumentExpressions = expression.substring(parametersStartIndex + 1, parametersEndIndex).split(",");
        }
        /*
         * We need to make sure the cache key contains the both the expression and the parameter types of the method
         * on which the SecurityCheck will apply
         * This is because the generated security check takes into account the parameter types in order to create the
         * proper calls to the bean method.
         * If however the expressions indicated that no parameters are passed to the bean method, we can just use the
         * expression as the cache key
         */
        String cacheKey = beanMethodArgumentExpressions.length > 0 ? expression + "-" + paramTypesDescriptor : expression;

        String cachedGeneratedClassName = alreadyGeneratedClasses.get(cacheKey);
        if (cachedGeneratedClassName != null) {
            return cachedGeneratedClassName;
        }

        Matcher matcher = BASIC_BEAN_METHOD_INVOCATION_PATTERN.matcher(expression);
        if (!matcher.find()) { // should never happen
            throw createGenericMalformedException(securedMethodInfo, expression);
        }

        String beanName = matcher.group(1);
        ClassInfo beanClassInfo = getClassInfoFromBeanName(beanName, index,
                springBeansNameToDotName, springBeansNameToClassInfo, expression, securedMethodInfo);

        String beanMethodName = matcher.group(2);
        MethodInfo matchingBeanMethod = determineMatchingBeanMethod(beanMethodName, beanMethodArgumentExpressions.length,
                beanClassInfo, securedMethodInfo, expression, beanName);

        String generatedClassName = "io.quarkus.spring.security.check." + beanClassInfo.name().withoutPackagePrefix() + "_"
                + HashUtil.sha1(cacheKey) + "_CheckFor_" + beanMethodName;
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(generatedClassName)
                .superClass(AbstractBeanMethodSecurityCheck.class)
                .build()) {

            /*
             * The generated classes will have a static getInstance method that will allow the creation of a single instance
             * This is done to avoid creating multiple objects for the same expression
             */

            FieldDescriptor instanceField = of(generatedClassName, "INSTANCE", generatedClassName);
            cc.getFieldCreator(instanceField).setModifiers(Modifier.STATIC | Modifier.PRIVATE);

            try (MethodCreator getInstance = cc.getMethodCreator("getInstance", generatedClassName)
                    .setModifiers(Modifier.STATIC | Modifier.PUBLIC)) {
                ResultHandle instance = getInstance.readStaticField(instanceField);
                BranchResult instanceNullBranch = getInstance.ifNull(instance);
                instanceNullBranch.falseBranch().returnValue(instance);
                BytecodeCreator instanceNullTrue = instanceNullBranch.trueBranch();
                ResultHandle newInstance = instanceNullTrue.newInstance(ofConstructor(generatedClassName));
                instanceNullTrue.writeStaticField(instanceField, newInstance);
                instanceNullTrue.returnValue(newInstance);
            }

            try (MethodCreator check = cc.getMethodCreator("check", boolean.class, SecurityIdentity.class, Object[].class)
                    .setModifiers(Modifier.PROTECTED)) {
                ResultHandle arcContainer = check
                        .invokeStaticMethod(ofMethod(Arc.class, "container", ArcContainer.class));
                ResultHandle instanceHandle = check.invokeInterfaceMethod(
                        ofMethod(ArcContainer.class, "instance", InstanceHandle.class, String.class),
                        arcContainer, check.load(beanName));
                ResultHandle bean = check
                        .invokeInterfaceMethod(ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);
                ResultHandle castedBean = check.checkCast(bean, beanClassInfo.name().toString());
                ResultHandle[] argHandles = new ResultHandle[beanMethodArgumentExpressions.length];

                for (int i = 0; i < beanMethodArgumentExpressions.length; i++) {
                    String argumentExpression = beanMethodArgumentExpressions[i];
                    String trimmedArgumentExpression = argumentExpression.trim();
                    if (argumentExpression.startsWith("'") && argumentExpression.endsWith("'")) { // hard coded string case
                        if (!DotNames.STRING.equals(matchingBeanMethod.parameters().get(i).name())) {
                            throw new IllegalArgumentException("Parameter with index " + i + " of method '" + beanMethodName
                                    + "' found in expression '" + trimmedArgumentExpression
                                    + "' in the @PreAuthorize annotation on method " + securedMethodInfo.name() + " of class "
                                    + securedMethodInfo.declaringClass() + " is not of type String");
                        }

                        argHandles[i] = check.load(argumentExpression.replace("'", ""));
                    } else if (trimmedArgumentExpression.matches(METHOD_PARAMETER_REGEX)) { // secured method's parameter case
                        Matcher parameterMatcher = METHOD_PARAMETER_PATTERN.matcher(trimmedArgumentExpression);
                        if (!parameterMatcher.find()) { // should never happen
                            throw createGenericMalformedException(securedMethodInfo, expression);
                        }

                        // this is the index index of the parameter we care about
                        int parameterIndex = getParameterIndex(securedMethodInfo, parameterMatcher.group(1), expression);

                        DotName expectedType = securedMethodInfo.parameters().get(parameterIndex).name();
                        if (!matchingBeanMethod.parameters().get(i).name().equals(expectedType)) {
                            throw new IllegalArgumentException("Parameter with index " + i + " of method '" + beanMethodName
                                    + "' found in expression '" + trimmedArgumentExpression
                                    + "' in the @PreAuthorize annotation on method " + securedMethodInfo.name() + " of class "
                                    + securedMethodInfo.declaringClass() + " is not of type " + expectedType);
                        }

                        /*
                         * the check method from AbstractBeanMethodSecurityCheck contains all parameters in an object array
                         * so we need to use that to read the value at runtime
                         */
                        ResultHandle methodArgsArrays = check.getMethodParam(1);
                        argHandles[i] = check.readArrayValue(methodArgsArrays, parameterIndex);
                    } else if (trimmedArgumentExpression.matches("(authentication.)?principal.username")) { // username use case
                        ResultHandle securityIdentity = check.getMethodParam(0);
                        ResultHandle principal = check.invokeInterfaceMethod(
                                ofMethod(SecurityIdentity.class, "getPrincipal", Principal.class), securityIdentity);

                        if (!DotNames.STRING.equals(matchingBeanMethod.parameters().get(i).name())) {
                            throw new IllegalArgumentException("Parameter with index " + i + " of method '" + beanMethodName
                                    + "' found in expression '" + trimmedArgumentExpression
                                    + "' in the @PreAuthorize annotation on method " + securedMethodInfo.name() + " of class "
                                    + securedMethodInfo.declaringClass() + " is not of type String");
                        }

                        ResultHandle username = check
                                .invokeInterfaceMethod(ofMethod(Principal.class, "getName", String.class), principal);
                        argHandles[i] = username;
                    } else {
                        throw createGenericMalformedException(securedMethodInfo, expression);
                    }
                }

                ResultHandle result;
                if (Modifier.isInterface(matchingBeanMethod.declaringClass().flags())) {
                    result = check.invokeInterfaceMethod(MethodDescriptor.of(matchingBeanMethod), castedBean, argHandles);
                } else {
                    result = check.invokeVirtualMethod(MethodDescriptor.of(matchingBeanMethod), castedBean, argHandles);
                }

                check.returnValue(result);
            }
        }

        beansReferencedInPreAuthorized.add(beanClassInfo.name().toString());
        alreadyGeneratedClasses.put(cacheKey, generatedClassName);
        return generatedClassName;
    }

    private String getParamTypesDescriptor(MethodInfo securedMethodInfo) {
        StringBuilder sb = new StringBuilder("(");
        for (Type type : securedMethodInfo.parameters()) {
            sb.append(DescriptorUtils.objectToDescriptor(type.name().toString()));
        }
        sb.append(")");
        return sb.toString();
    }

    private MethodInfo determineMatchingBeanMethod(String methodName, int methodParametersSize, ClassInfo beanClassInfo,
            MethodInfo securedMethodInfo, String expression, String beanName) {
        MethodInfo matchingBeanClassMethod = null;
        for (MethodInfo candidateMethod : beanClassInfo.methods()) {
            if (candidateMethod.name().equals(methodName) &&
                    Modifier.isPublic(candidateMethod.flags()) &&
                    DotNames.PRIMITIVE_BOOLEAN.equals(candidateMethod.returnType().name()) &&
                    candidateMethod.parameters().size() == methodParametersSize) {
                if (matchingBeanClassMethod == null) {
                    matchingBeanClassMethod = candidateMethod;
                } else {
                    throw new IllegalArgumentException(
                            "Could not match a unique method name '" + methodName + "' for bean named " + beanName
                                    + " with class " + beanClassInfo.name() + " Offending expression is " +
                                    expression + " of @PreAuthorize on method '" + methodName + "' of class "
                                    + securedMethodInfo.declaringClass());
                }
            }
        }
        if (matchingBeanClassMethod == null) {
            throw new IllegalArgumentException(
                    "Could not find a public, boolean returning method named '" + methodName + "' for bean named " + beanName
                            + " with class " + beanClassInfo.name() + " Offending expression is " +
                            expression + " of @PreAuthorize on method '" + methodName + "' of class "
                            + securedMethodInfo.declaringClass());
        }
        return matchingBeanClassMethod;
    }

}
