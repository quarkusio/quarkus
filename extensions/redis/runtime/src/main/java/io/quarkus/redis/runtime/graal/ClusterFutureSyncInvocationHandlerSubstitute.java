package io.quarkus.redis.runtime.graal;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Remove the usage of {@link MethodHandle}
 */
@TargetClass(className = "io.lettuce.core.cluster.ClusterFutureSyncInvocationHandler")
final public class ClusterFutureSyncInvocationHandlerSubstitute {
    @Substitute
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }

    @Delete
    private static MethodHandle lookupDefaultMethod(Method method) {
        return null;
    }
}
