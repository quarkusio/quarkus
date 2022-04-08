package io.quarkus.jaxb.runtime.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.sun.xml.bind.v2.model.nav.ReflectionNavigator")
final class Target_com_sun_xml_bind_v2_model_nav_ReflectionNavigator {

    @Substitute
    public Field[] getEnumConstants(Class clazz) {
        try {
            Object[] values = clazz.getEnumConstants();
            Field[] fields = new Field[values.length];
            for (int i = 0; i < values.length; i++) {
                fields[i] = clazz.getField(((Enum) values[i]).name());
            }
            return fields;
        } catch (NoSuchFieldException e) {
            // impossible
            throw new NoSuchFieldError(clazz.getName() + ": " + e.getMessage());
        }
    }

}

@TargetClass(className = "com.sun.xml.bind.v2.runtime.reflect.opt.AccessorInjector")
@Substitute
final class Target_com_sun_xml_bind_v2_runtime_reflect_opt_AccessorInjector {

    /**
     * Loads the optimized class and returns it.
     *
     * @return null
     *         if it fails for some reason.
     */
    @Substitute()
    public static Class<?> prepare(
            Class beanClass, String templateClassName, String newClassName, String... replacements) {
        return null;
    }

}

@TargetClass(className = "com.sun.xml.bind.v2.model.annotation.LocatableAnnotation")
final class Target_com_sun_xml_bind_v2_model_annotation_LocatableAnnotation {

    @Substitute
    public static <A extends Annotation> A create(A annotation, Locatable parentSourcePos) {
        return annotation;
    }

    @Substitute
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        throw new RuntimeException("Not implemented");
    }

    @TargetClass(className = "com.sun.xml.bind.v2.model.annotation.Locatable")
    static final class Locatable {

    }

}

class JAXBSubstitutions {
}
