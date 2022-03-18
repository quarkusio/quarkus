package io.quarkus.avro.runtime.graal;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.ResolvingDecoder;
import org.apache.avro.util.WeakIdentityHashMap;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.graal.JDK17OrLater;

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

@TargetClass(className = "org.apache.avro.reflect.ReflectionUtil", onlyWith = JDK17OrLater.class)
final class Target_org_apache_avro_reflect_ReflectionUtil {

    @Substitute
    public static <V, R> Function<V, R> getConstructorAsFunction(Class<V> parameterClass, Class<R> clazz) {
        // Cannot use the method handle approach as it uses ProtectionDomain which got removed.
        try {
            Constructor<R> constructor = clazz.getConstructor(parameterClass);
            return new Function<V, R>() {
                @Override
                public R apply(V v) {
                    try {
                        return constructor.newInstance(v);
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to create new instance for " + clazz, e);
                    }
                }
            };
        } catch (Throwable t) {
            // if something goes wrong, do not provide a Function instance
            return null;
        }
    }
}

class AvroSubstitutions {
}
