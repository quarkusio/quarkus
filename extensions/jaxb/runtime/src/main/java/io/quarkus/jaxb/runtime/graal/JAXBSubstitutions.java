package io.quarkus.jaxb.runtime.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.sun.xml.bind.v2.model.nav.ReflectionNavigator", onlyWith = Target_com_sun_xml_bind_v2_model_nav_ReflectionNavigator.Selector.class)
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

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.sun.xml.bind.v2.model.nav.ReflectionNavigator");
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}

@TargetClass(className = "com.sun.xml.bind.v2.runtime.reflect.opt.AccessorInjector", onlyWith = Target_com_sun_xml_bind_v2_runtime_reflect_opt_AccessorInjector.Selector.class)
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

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class<?> c = Class.forName("com.sun.xml.bind.v2.runtime.reflect.opt.AccessorInjector");
                c.getDeclaredMethod("prepare", Class.class, Class.class, String.class, String.class, String[].class);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}

@TargetClass(className = "com.sun.xml.internal.bind.v2.model.annotation.LocatableAnnotation", onlyWith = Target_com_sun_xml_internal_bind_v2_model_annotation_LocatableAnnotation.Selector.class)
final class Target_com_sun_xml_internal_bind_v2_model_annotation_LocatableAnnotation {

    @Substitute
    public static <A extends Annotation> A create(A annotation, Locatable parentSourcePos) {
        return annotation;
    }

    @Substitute
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        throw new RuntimeException("Not implemented");
    }

    @TargetClass(className = "com.sun.xml.internal.bind.v2.model.annotation.Locatable", onlyWith = Target_com_sun_xml_internal_bind_v2_model_annotation_LocatableAnnotation.Selector.class)
    static final class Locatable {

    }

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.sun.xml.internal.bind.v2.model.annotation.LocatableAnnotation");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}

@TargetClass(className = "com.sun.xml.internal.bind.v2.model.nav.ReflectionNavigator", onlyWith = Target_com_sun_xml_internal_bind_v2_model_nav_ReflectionNavigator.Selector.class)
final class Target_com_sun_xml_internal_bind_v2_model_nav_ReflectionNavigator {

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

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.sun.xml.internal.bind.v2.model.nav.ReflectionNavigator");
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}

@TargetClass(className = "com.sun.xml.internal.bind.v2.runtime.reflect.opt.AccessorInjector", onlyWith = Target_com_sun_xml_internal_bind_v2_runtime_reflect_opt_AccessorInjector.Selector.class)
@Substitute
final class Target_com_sun_xml_internal_bind_v2_runtime_reflect_opt_AccessorInjector {

    /**
     * Loads the optimized class and returns it.
     *
     * @return null
     *         if it fails for some reason.
     */
    @Substitute
    public static Class<?> prepare(
            Class beanClass, String templateClassName, String newClassName, String... replacements) {
        return null;
    }

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.sun.xml.internal.bind.v2.runtime.reflect.opt.AccessorInjector");
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}

class JAXBSubstitutions {
}
