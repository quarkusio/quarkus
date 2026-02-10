package io.quarkus.spring.security.deployment;

import java.util.Set;

import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.gizmo2.Jandex2Gizmo;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
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
        Gizmo.create(classOutput).class_(generatedClassName, cc -> {
            cc.implements_(StringPropertyAccessor.class);
            cc.addAnnotation(Singleton.class);
            cc.defaultConstructor();

            cc.method("access", mc -> {
                mc.public_();
                mc.returning(String.class);
                ParamVar objectParam = mc.parameter("obj", Object.class);
                ParamVar propertyParam = mc.parameter("property", String.class);

                mc.body(bc -> {
                    LocalVar castedObjectParam = bc.localVar("castedObj",
                            bc.cast(objectParam, Jandex2Gizmo.classDescOf(className)));
                    for (FieldInfo fieldInfo : properties) {
                        Expr propertyName = Const.of(fieldInfo.name());
                        Expr propertyNameEquals = bc.invokeVirtual(
                                MethodDesc.of(Object.class, "equals", boolean.class, Object.class),
                                propertyName, propertyParam);
                        bc.if_(propertyNameEquals, trueBlock -> {
                            Expr result = trueBlock.invokeVirtual(
                                    ClassMethodDesc.of(Jandex2Gizmo.classDescOf(className),
                                            "get" + JavaBeanUtil.capitalize(fieldInfo.name()),
                                            String.class),
                                    castedObjectParam);
                            trueBlock.return_(result);
                        });
                    }
                    bc.throw_(IllegalArgumentException.class, "Property unknown");
                });
            });
        });
        return generatedClassName;
    }
}
