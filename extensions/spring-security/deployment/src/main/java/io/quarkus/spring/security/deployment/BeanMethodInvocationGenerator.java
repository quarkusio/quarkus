package io.quarkus.spring.security.deployment;

import static io.quarkus.spring.security.deployment.SpringSecurityProcessorUtil.BASIC_BEAN_METHOD_INVOCATION_PATTERN;
import static io.quarkus.spring.security.deployment.SpringSecurityProcessorUtil.createGenericMalformedException;
import static io.quarkus.spring.security.deployment.SpringSecurityProcessorUtil.getClassInfoFromBeanName;
import static io.quarkus.spring.security.deployment.SpringSecurityProcessorUtil.getParameterIndex;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Modifier;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.util.HashUtil;
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
            beanMethodArgumentExpressions = expression.substring(parametersStartIndex + 1, parametersEndIndex).trim()
                    .split("\\s*,\\s*");
            ;
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

        final String[] finalBeanMethodArgumentExpressions = beanMethodArgumentExpressions;
        final boolean[] checkRequiresMethodArguments = { false };

        Gizmo.create(classOutput).class_(generatedClassName, cc -> {
            cc.extends_(AbstractBeanMethodSecurityCheck.class);
            cc.defaultConstructor();

            /*
             * The generated classes will have a static getInstance method that will allow the creation of a single instance
             * This is done to avoid creating multiple objects for the same expression
             */

            ClassDesc generatedClassDesc = ClassDesc.of(generatedClassName);
            FieldDesc instanceFieldDesc = FieldDesc.of(generatedClassDesc, "INSTANCE", generatedClassDesc);
            cc.staticField("INSTANCE", sfc -> {
                sfc.private_();
                sfc.setType(generatedClassDesc);
            });

            cc.staticMethod("getInstance", smc -> {
                smc.public_();
                smc.returning(generatedClassDesc);
                smc.body(bc -> {
                    Expr instance = bc.getStaticField(instanceFieldDesc);
                    bc.ifElse(bc.isNotNull(instance),
                            trueBlock -> {
                                trueBlock.return_(trueBlock.getStaticField(instanceFieldDesc));
                            },
                            falseBlock -> {
                                LocalVar newInstance = falseBlock.localVar("newInstance",
                                        falseBlock.new_(ConstructorDesc.of(generatedClassDesc)));
                                falseBlock.setStaticField(instanceFieldDesc, newInstance);
                                falseBlock.return_(newInstance);
                            });
                });
            });

            cc.method("check", mc -> {
                mc.protected_();
                mc.returning(boolean.class);
                ParamVar securityIdentityParam = mc.parameter("securityIdentity", SecurityIdentity.class);
                ParamVar methodArgsParam = mc.parameter("methodArgs", Object[].class);

                mc.body(bc -> {
                    LocalVar arcContainer = bc.localVar("arcContainer", bc
                            .invokeStatic(MethodDesc.of(Arc.class, "container", ArcContainer.class)));
                    LocalVar instanceHandle = bc.localVar("instanceHandle", bc.invokeInterface(
                            MethodDesc.of(ArcContainer.class, "instance", InstanceHandle.class, String.class),
                            arcContainer, Const.of(beanName)));
                    LocalVar bean = bc.localVar("bean", bc
                            .invokeInterface(MethodDesc.of(InstanceHandle.class, "get", Object.class), instanceHandle));
                    LocalVar castedBean = bc.localVar("castedBean", bc.cast(bean, classDescOf(beanClassInfo)));

                    List<Expr> argHandles = new ArrayList<>(finalBeanMethodArgumentExpressions.length);

                    for (int i = 0; i < finalBeanMethodArgumentExpressions.length; i++) {
                        String argumentExpression = finalBeanMethodArgumentExpressions[i];
                        String trimmedArgumentExpression = argumentExpression.trim();
                        if (argumentExpression.startsWith("'") && argumentExpression.endsWith("'")) { // hard coded string case
                            if (!DotNames.STRING.equals(matchingBeanMethod.parameterType(i).name())) {
                                throw new IllegalArgumentException("Parameter with index " + i + " of method '" + beanMethodName
                                        + "' found in expression '" + trimmedArgumentExpression
                                        + "' in the @PreAuthorize annotation on method " + securedMethodInfo.name()
                                        + " of class "
                                        + securedMethodInfo.declaringClass() + " is not of type String");
                            }

                            argHandles.add(Const.of(argumentExpression.replace("'", "")));
                        } else if (trimmedArgumentExpression.matches(METHOD_PARAMETER_REGEX)) { // secured method's parameter case
                            checkRequiresMethodArguments[0] = true;
                            Matcher parameterMatcher = METHOD_PARAMETER_PATTERN.matcher(trimmedArgumentExpression);
                            if (!parameterMatcher.find()) { // should never happen
                                throw createGenericMalformedException(securedMethodInfo, expression);
                            }

                            // this is the index of the parameter we care about
                            int parameterIndex = getParameterIndex(securedMethodInfo, parameterMatcher.group(1), expression);

                            DotName expectedType = securedMethodInfo.parameterType(parameterIndex).name();
                            if (!matchingBeanMethod.parameterType(i).name().equals(expectedType)) {
                                throw new IllegalArgumentException("Parameter with index " + i + " of method '" + beanMethodName
                                        + "' found in expression '" + trimmedArgumentExpression
                                        + "' in the @PreAuthorize annotation on method " + securedMethodInfo.name()
                                        + " of class "
                                        + securedMethodInfo.declaringClass() + " is not of type " + expectedType);
                            }

                            /*
                             * the check method from AbstractBeanMethodSecurityCheck contains all parameters in an object array
                             * so we need to use that to read the value at runtime
                             */
                            argHandles.add(bc.localVar("methodArg" + parameterIndex,
                                    bc.get(methodArgsParam.elem(parameterIndex))));
                        } else if (trimmedArgumentExpression
                                .matches("(authentication.)?principal.username")) { // username use case
                            LocalVar principal = bc.localVar("principal", bc.invokeInterface(
                                    MethodDesc.of(SecurityIdentity.class, "getPrincipal", Principal.class),
                                    securityIdentityParam));

                            if (!DotNames.STRING.equals(matchingBeanMethod.parameterType(i).name())) {
                                throw new IllegalArgumentException("Parameter with index " + i + " of method '" + beanMethodName
                                        + "' found in expression '" + trimmedArgumentExpression
                                        + "' in the @PreAuthorize annotation on method " + securedMethodInfo.name()
                                        + " of class "
                                        + securedMethodInfo.declaringClass() + " is not of type String");
                            }

                            LocalVar username = bc.localVar("username", bc
                                    .invokeInterface(MethodDesc.of(Principal.class, "getName", String.class), principal));
                            argHandles.add(username);
                        } else {
                            throw createGenericMalformedException(securedMethodInfo, expression);
                        }
                    }

                    Expr result;
                    if (Modifier.isInterface(matchingBeanMethod.declaringClass().flags())) {
                        result = bc.invokeInterface(methodDescOf(matchingBeanMethod), castedBean, argHandles);
                    } else {
                        result = bc.invokeVirtual(methodDescOf(matchingBeanMethod), castedBean, argHandles);
                    }

                    bc.return_(result);
                });
            });

            if (checkRequiresMethodArguments[0]) {
                cc.method("requiresMethodArguments", rmc -> {
                    rmc.public_();
                    rmc.returning(boolean.class);
                    rmc.body(bc -> {
                        bc.return_(true);
                    });
                });
            }
        });

        beansReferencedInPreAuthorized.add(beanClassInfo.name().toString());
        alreadyGeneratedClasses.put(cacheKey, generatedClassName);
        return generatedClassName;
    }

    private String getParamTypesDescriptor(MethodInfo securedMethodInfo) {
        StringBuilder sb = new StringBuilder("(");
        for (Type type : securedMethodInfo.parameterTypes()) {
            sb.append(classDescOf(type).descriptorString());
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
                    candidateMethod.parametersCount() == methodParametersSize) {
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
