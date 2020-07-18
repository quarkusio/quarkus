package io.quarkus.avro.graal;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.avro.generic.IndexedRecord;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Inject;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.apache.avro.reflect.ReflectionUtil")
final class Target_org_apache_avro_reflect_ReflectionUtil {

    /**
     * Use reflection instead of method handles
     */
    @Substitute
    public static <V, R> Function<V, R> getConstructorAsFunction(Class<V> parameterClass, Class<R> clazz) {
        try {
            Constructor<R> constructor = clazz.getConstructor(parameterClass);
            return new Function<V, R>() {
                @Override
                public R apply(V v) {
                    try {
                        return constructor.newInstance(v);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } catch (Throwable t) {
            // if something goes wrong, do not provide a Function instance
            return null;
        }
    }

}

@TargetClass(className = "org.apache.avro.reflect.ReflectData")
final class Target_org_apache_avro_reflect_ReflectData {

    @Inject
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.None)
    Map<Class<?>, Target_org_apache_avro_reflect_ReflectData_ClassAccessorData> ACCESSORS;

    @Substitute
    private Target_org_apache_avro_reflect_ReflectData_ClassAccessorData getClassAccessorData(Class<?> c) {
        if (ACCESSORS == null) {
            ACCESSORS = new HashMap<>();
        }

        Map<Class<?>, Target_org_apache_avro_reflect_ReflectData_ClassAccessorData> map = ACCESSORS;
        Target_org_apache_avro_reflect_ReflectData_ClassAccessorData o = map.get(c);
        if (o == null) {
            if (!IndexedRecord.class.isAssignableFrom(c)) {
                Target_org_apache_avro_reflect_ReflectData_ClassAccessorData d = new Target_org_apache_avro_reflect_ReflectData_ClassAccessorData(
                        c);
                map.put(c, d);
            }
            return null;
        }
        return o;
    }
}

@TargetClass(className = "org.apache.avro.reflect.ReflectData", innerClass = "ClassAccessorData")
final class Target_org_apache_avro_reflect_ReflectData_ClassAccessorData<T> {
    // Just provide access to "ReflectData.ClassAccessorData"

    @Alias
    public Target_org_apache_avro_reflect_ReflectData_ClassAccessorData(Class<?> c) {

    }

}

class AvroSubstitutions {
}
