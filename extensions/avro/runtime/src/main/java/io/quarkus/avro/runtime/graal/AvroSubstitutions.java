package io.quarkus.avro.runtime.graal;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.ResolvingDecoder;
import org.apache.avro.util.WeakIdentityHashMap;
import org.graalvm.home.Version;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Inject;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.apache.avro.reflect.ReflectionUtil", onlyWith = GraalVM20OrEarlier.class)
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

@TargetClass(className = "org.apache.avro.reflect.ReflectData", onlyWith = GraalVM20OrEarlier.class)
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

@TargetClass(className = "org.apache.avro.reflect.ReflectData", innerClass = "ClassAccessorData", onlyWith = GraalVM20OrEarlier.class)
final class Target_org_apache_avro_reflect_ReflectData_ClassAccessorData<T> {
    // Just provide access to "ReflectData.ClassAccessorData"

    @Alias
    public Target_org_apache_avro_reflect_ReflectData_ClassAccessorData(Class<?> c) {

    }

}

@TargetClass(className = "org.apache.avro.generic.GenericDatumReader")
final class Target_org_apache_avro_generic_GenericDatumReader {

    @Alias
    private GenericData data;
    @Alias
    private Schema actual;
    @Alias
    private Schema expected;
    @Alias
    private DatumReader<?> fastDatumReader;
    @Alias
    private ResolvingDecoder creatorResolver;
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private Thread creator;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static ThreadLocal<Map<Schema, Map<Schema, ResolvingDecoder>>> RESOLVER_CACHE = ThreadLocal.withInitial(
            WeakIdentityHashMap::new);
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private Map<Schema, Class> stringClassCache;
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private Map<Class, Constructor> stringCtorCache;

    @Substitute
    protected Target_org_apache_avro_generic_GenericDatumReader(GenericData data) {
        this.fastDatumReader = null;
        this.creatorResolver = null;
        this.stringClassCache = new IdentityHashMap();
        this.stringCtorCache = new HashMap();
        this.data = data;
        this.creator = Thread.currentThread();
    }

}

class GraalVM20OrEarlier implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return Version.getCurrent().compareTo(21) < 0;
    }
}

class AvroSubstitutions {
}
