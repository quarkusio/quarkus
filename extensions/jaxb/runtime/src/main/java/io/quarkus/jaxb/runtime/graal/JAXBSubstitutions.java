package io.quarkus.jaxb.runtime.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlSeeAlso;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.sun.xml.bind.v2.model.annotation.Locatable;
import com.sun.xml.bind.v2.model.annotation.LocatableAnnotation;

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

@TargetClass(className = "com.sun.xml.bind.v2.model.annotation.RuntimeInlineAnnotationReader")
final class Target_com_sun_xml_bind_v2_model_annotation_RuntimeInlineAnnotationReader {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.NewInstance, declClass = HashMap.class)
    private Map<Class<? extends Annotation>, Map<Package, Annotation>> packageCache;

    @Substitute
    public <A extends Annotation> A getFieldAnnotation(Class<A> annotation, Field field, Locatable srcPos) {
        return field.getAnnotation(annotation);
    }

    @Substitute
    public Annotation[] getAllFieldAnnotations(Field field, Locatable srcPos) {
        return field.getAnnotations();
    }

    @Substitute
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotation, Method method, Locatable srcPos) {
        return method.getAnnotation(annotation);
    }

    @Substitute
    public Annotation[] getAllMethodAnnotations(Method method, Locatable srcPos) {
        return method.getAnnotations();
    }

    @Substitute
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getMethodParameterAnnotation(Class<A> annotation, Method method, int paramIndex,
            Locatable srcPos) {
        Annotation[] pa = method.getParameterAnnotations()[paramIndex];
        for (Annotation a : pa) {
            if (a.annotationType() == annotation)
                return (A) a;
        }
        return null;
    }

    @Substitute
    public <A extends Annotation> A getClassAnnotation(Class<A> a, Class clazz, Locatable srcPos) {
        A ann = ((Class<?>) clazz).getAnnotation(a);
        return (ann != null && ann.annotationType() == XmlSeeAlso.class) ? LocatableAnnotation.create(ann, srcPos) : ann;
    }

    @Substitute
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getPackageAnnotation(Class<A> a, Class clazz, Locatable srcPos) {
        Package p = clazz.getPackage();
        if (p == null)
            return null;

        Map<Package, Annotation> cache = packageCache.get(a);
        if (cache == null) {
            cache = new HashMap<>();
            packageCache.put(a, cache);
        }

        if (cache.containsKey(p))
            return (A) cache.get(p);
        else {
            A ann = p.getAnnotation(a);
            cache.put(p, ann);
            return ann;
        }
    }
}

class JAXBSubstitutions {
}
