package io.quarkus.restclient.runtime.graal;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @author <a href="http://kenfinnigan.me">Ken Finnigan</a>
 */
@TargetClass(className = "org.jboss.resteasy.microprofile.client.header.ComputedHeaderValueFiller")
final class ComputedHeaderValueFillerReplacement {
    @Substitute
    private MethodHandle createMethodHandle(Method method, Object clientProxy) {
        return null;
    }
}
