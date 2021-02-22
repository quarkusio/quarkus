package io.quarkus.arc.deployment.devconsole;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.ObserverInfo;
import io.quarkus.arc.runtime.ArcContainerSupplier;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    public DevConsoleRuntimeTemplateInfoBuildItem collectBeanInfo(ArcRecorder recorder) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("arcContainer",
                new ArcContainerSupplier());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectBeanInfo(ValidationPhaseBuildItem validationPhaseBuildItem) {
        BeanDeploymentValidator.ValidationContext validationContext = validationPhaseBuildItem.getContext();
        DevBeanInfos beanInfos = new DevBeanInfos();
        for (BeanInfo beanInfo : validationContext.beans()) {
            beanInfos.addBean(createBeanInfo(beanInfo));
        }
        for (BeanInfo beanInfo : validationContext.removedBeans()) {
            beanInfos.addRemovedBean(createBeanInfo(beanInfo));
        }
        for (ObserverInfo observerInfo : validationContext.get(BuildExtension.Key.OBSERVERS)) {
            beanInfos.addObserver(createDevInfo(observerInfo));
        }
        return new DevConsoleTemplateInfoBuildItem("devBeanInfos", beanInfos);
    }

    private DevBeanInfo createBeanInfo(BeanInfo beanInfo) {
        List<ClassName> qualifiers = new ArrayList<>();
        for (AnnotationInstance qualAnnotation : beanInfo.getQualifiers()) {
            qualifiers.add(new ClassName(qualAnnotation.name().toString()));
        }
        ClassName scope = new ClassName(beanInfo.getScope().getDotName().toString());
        Optional<AnnotationTarget> target = beanInfo.getTarget();
        if (target.isPresent()) {
            AnnotationTarget annotated = target.get();
            String methodName = null;
            ClassName type = null;
            ClassName providerClass = null;
            DevBeanKind kind = null;
            if (annotated.kind() == Kind.METHOD) {
                MethodInfo method = annotated.asMethod();
                methodName = method.name();
                type = new ClassName(method.returnType().toString());
                providerClass = new ClassName(method.declaringClass().toString());
                kind = DevBeanKind.METHOD;
            } else if (annotated.kind() == Kind.FIELD) {
                FieldInfo field = annotated.asField();
                methodName = field.name();
                type = new ClassName(field.type().toString());
                providerClass = new ClassName(field.declaringClass().toString());
                kind = DevBeanKind.FIELD;
            } else if (annotated.kind() == Kind.CLASS) {
                ClassInfo klass = annotated.asClass();
                type = new ClassName(klass.name().toString());
                kind = DevBeanKind.CLASS;
            }
            return new DevBeanInfo(providerClass, methodName, type, qualifiers, scope, kind);
        } else {
            return new DevBeanInfo(null, null, new ClassName(beanInfo.getBeanClass().toString()),
                    qualifiers,
                    scope, DevBeanKind.SYNTHETIC);
        }
    }

    private DevObserverInfo createDevInfo(ObserverInfo observer) {
        List<ClassName> qualifiers = new ArrayList<>();
        ClassName name = null;
        String methodName = null;
        for (AnnotationInstance qualAnnotation : observer.getQualifiers()) {
            qualifiers.add(new ClassName(qualAnnotation.name().toString()));
        }
        if (observer.getDeclaringBean() != null) {
            name = new ClassName(observer.getObserverMethod().declaringClass().name().toString());
            methodName = observer.getObserverMethod().name();
        }
        return new DevObserverInfo(name, methodName, observer.getObservedType().toString(), qualifiers, observer.getPriority(),
                observer.isAsync(), observer.getReception(), observer.getTransactionPhase());
    }

}
