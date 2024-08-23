package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.inject.build.compatible.spi.InterceptorInfo;
import jakarta.enterprise.inject.build.compatible.spi.InvokerFactory;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.build.compatible.spi.MethodConfig;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.Types;
import jakarta.enterprise.inject.build.compatible.spi.Validation;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;

import org.jboss.jandex.DotName;

class DotNames {
    static final DotName ANNOTATION = DotName.createSimple(Annotation.class);
    static final DotName OBJECT = DotName.createSimple(Object.class);

    // common annotations

    static final DotName PRIORITY = DotName.createSimple(Priority.class);

    // lang model

    static final DotName CLASS_INFO = DotName.createSimple(ClassInfo.class);
    static final DotName METHOD_INFO = DotName.createSimple(MethodInfo.class);
    static final DotName FIELD_INFO = DotName.createSimple(FieldInfo.class);

    // extension API

    static final DotName BUILD_COMPATIBLE_EXTENSION = DotName.createSimple(BuildCompatibleExtension.class);

    static final DotName DISCOVERY = DotName.createSimple(Discovery.class);
    static final DotName ENHANCEMENT = DotName.createSimple(Enhancement.class);
    static final DotName REGISTRATION = DotName.createSimple(Registration.class);
    static final DotName SYNTHESIS = DotName.createSimple(Synthesis.class);
    static final DotName VALIDATION = DotName.createSimple(Validation.class);

    static final DotName CLASS_CONFIG = DotName.createSimple(ClassConfig.class);
    static final DotName METHOD_CONFIG = DotName.createSimple(MethodConfig.class);
    static final DotName FIELD_CONFIG = DotName.createSimple(FieldConfig.class);

    static final DotName BEAN_INFO = DotName.createSimple(BeanInfo.class);
    static final DotName INTERCEPTOR_INFO = DotName.createSimple(InterceptorInfo.class);
    static final DotName OBSERVER_INFO = DotName.createSimple(ObserverInfo.class);

    static final DotName INVOKER_FACTORY = DotName.createSimple(InvokerFactory.class);
    static final DotName MESSAGES = DotName.createSimple(Messages.class);
    static final DotName META_ANNOTATIONS = DotName.createSimple(MetaAnnotations.class);
    static final DotName SCANNED_CLASSES = DotName.createSimple(ScannedClasses.class);
    static final DotName SYNTHETIC_COMPONENTS = DotName.createSimple(SyntheticComponents.class);
    static final DotName TYPES = DotName.createSimple(Types.class);
}
