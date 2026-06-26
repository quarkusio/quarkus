package io.quarkus.spring.web.deployment;

import java.lang.constant.ClassDesc;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.GenericType;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.TypeArgument;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.creator.ModifierFlag;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.runtime.util.HashUtil;

abstract class AbstractExceptionMapperGenerator {

    protected static final DotName RESPONSE_STATUS = DotName
            .createSimple("org.springframework.web.bind.annotation.ResponseStatus");

    protected final DotName exceptionDotName;
    protected final ClassOutput classOutput;

    private final boolean isResteasyClassic;

    AbstractExceptionMapperGenerator(DotName exceptionDotName, ClassOutput classOutput,
            boolean isResteasyClassic) {
        this.exceptionDotName = exceptionDotName;
        this.classOutput = classOutput;
        this.isResteasyClassic = isResteasyClassic;
    }

    abstract void generateMethodBody(BlockCreator bc, Expr thisRef, Expr exceptionParam);

    String generate() {
        String generatedClassName = "io.quarkus.spring.web.mappers." + exceptionDotName.withoutPackagePrefix() + "_Mapper_"
                + HashUtil.sha1(exceptionDotName.toString());
        String exceptionClassName = exceptionDotName.toString();
        ClassDesc exceptionClassDesc = ClassDesc.of(exceptionClassName);
        ClassDesc generatedClassDesc = ClassDesc.of(generatedClassName);

        Gizmo gizmo = Gizmo.create(classOutput);
        gizmo.class_(generatedClassName, cc -> {
            cc.implements_(GenericType.ofClass(ExceptionMapper.class,
                    TypeArgument.of(exceptionClassDesc)));
            cc.defaultConstructor();

            preGenerateMethodBody(cc);

            // toResponse method with the specific exception type
            cc.method("toResponse", mc -> {
                mc.public_();
                mc.returning(Response.class);
                var exceptionParam = mc.parameter("exception", exceptionClassDesc);
                mc.body(bc -> {
                    generateMethodBody(bc, mc.this_(), exceptionParam);
                });
            });

            // bridge method
            cc.method("toResponse", mc -> {
                mc.public_();
                mc.addFlag(ModifierFlag.BRIDGE);
                mc.addFlag(ModifierFlag.SYNTHETIC);
                mc.returning(Response.class);
                var throwableParam = mc.parameter("throwable", Throwable.class);
                mc.body(bc -> {
                    Expr castedObject = bc.cast(throwableParam, exceptionClassDesc);
                    Expr result = bc.invokeVirtual(
                            ClassMethodDesc.of(generatedClassDesc, "toResponse",
                                    ClassDesc.of(Response.class.getName()), exceptionClassDesc),
                            mc.this_(), castedObject);
                    bc.return_(result);
                });
            });
        });

        if (isResteasyClassic) {
            String generatedSubtypeClassName = "io.quarkus.spring.web.mappers.Subtype" + exceptionDotName.withoutPackagePrefix()
                    + "Mapper_" + HashUtil.sha1(exceptionDotName.toString());
            // additionally generate a dummy subtype to get past the RESTEasy's ExceptionMapper check for synthetic classes
            gizmo.class_(generatedSubtypeClassName, cc -> {
                cc.extends_(generatedClassDesc);
                cc.addAnnotation(Provider.class);
                cc.defaultConstructor();
            });

            return generatedSubtypeClassName;
        }
        return generatedClassName;
    }

    protected void preGenerateMethodBody(ClassCreator cc) {

    }

    protected int getHttpStatusFromAnnotation(AnnotationInstance responseStatusInstance) {
        AnnotationValue code = responseStatusInstance.value("code");
        if (code != null) {
            return enumValueToHttpStatus(code.asString());
        }

        AnnotationValue value = responseStatusInstance.value();
        if (value != null) {
            return enumValueToHttpStatus(value.asString());
        }

        return 500; // the default value of @ResponseStatus
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private int enumValueToHttpStatus(String enumValue) {
        try {
            Class<?> httpStatusClass = Class.forName("org.springframework.http.HttpStatus");
            Enum correspondingEnum = Enum.valueOf((Class<Enum>) httpStatusClass, enumValue);
            Method valueMethod = httpStatusClass.getDeclaredMethod("value");
            return (int) valueMethod.invoke(correspondingEnum);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No spring web dependency found on the build classpath");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
