package io.quarkus.spring.security.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.Set;

import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.spring.security.runtime.interceptor.accessor.StringPropertyAccessor;

final class StringPropertyAccessorGenerator {

    private StringPropertyAccessorGenerator() {
    }

    static String getAccessorClassName(DotName className) {
        return "io.quarkus.spring.security.accessor." + className.withoutPackagePrefix() + "_"
                + HashUtil.sha1(className.toString()) + "_Accessor";
    }

    /**
     * Generates a class like the following:
     *
     * <pre>
     * &#64;Singleton
     * public class Person_1234_Accessor implements StringPropertyAccessor {
     *
     *     public String access(Object obj, String property) {
     *         Person person = (Person) obj;
     *         if ("name".equals(property)) {
     *             return person.getName();
     *         }
     *         if ("lastName".equals(property)) {
     *             return person.getLastName();
     *         }
     *         throw new IllegalArgumentException("Unknown property '" + name + "'");
     *     }
     * }
     * </pre>
     *
     * This generated class is used by
     * {@link io.quarkus.spring.security.runtime.interceptor.check.PrincipalNameFromParameterObjectSecurityCheck}
     * to access fields of the object referenced by security expressions
     */
    static String generate(DotName className, Set<FieldInfo> properties, ClassOutput classOutput) {
        String generatedClassName = getAccessorClassName(className);
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(generatedClassName)
                .interfaces(StringPropertyAccessor.class)
                .build()) {

            cc.addAnnotation(Singleton.class);

            try (MethodCreator access = cc.getMethodCreator("access", String.class.getName(), Object.class, String.class)) {
                ResultHandle objectParam = access.getMethodParam(0);
                ResultHandle propertyParam = access.getMethodParam(1);
                ResultHandle castedObjectParam = access.checkCast(objectParam, className.toString());
                for (FieldInfo fieldInfo : properties) {
                    ResultHandle propertyName = access.load(fieldInfo.name());
                    ResultHandle propertyNameEquals = access.invokeVirtualMethod(
                            ofMethod(Object.class, "equals", boolean.class, Object.class),
                            propertyName, propertyParam);
                    BranchResult propertyNameEqualsBranch = access.ifNonZero(propertyNameEquals);
                    BytecodeCreator propertyNameEqualsTrue = propertyNameEqualsBranch.trueBranch();
                    ResultHandle result = propertyNameEqualsTrue.invokeVirtualMethod(
                            ofMethod(className.toString(), "get" + JavaBeanUtil.capitalize(fieldInfo.name()),
                                    String.class.getName()),
                            castedObjectParam);
                    propertyNameEqualsTrue.returnValue(result);
                }
                access.throwException(IllegalArgumentException.class, "Property unknown");
            }
        }
        return generatedClassName;
    }
}
