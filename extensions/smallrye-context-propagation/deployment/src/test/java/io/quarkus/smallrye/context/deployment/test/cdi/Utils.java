package io.quarkus.smallrye.context.deployment.test.cdi;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import io.quarkus.arc.ClientProxy;
import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.SmallRyeThreadContext;

public class Utils {

    private Utils() {
    }

    // util methods
    public static SmallRyeManagedExecutor unwrapExecutor(ManagedExecutor executor) {
        if (executor instanceof ClientProxy) {
            return (SmallRyeManagedExecutor) ((ClientProxy) executor).arc_contextualInstance();
        } else {
            throw new IllegalStateException("Injected proxies are expected to be instance of ClientProxy");
        }
    }

    public static SmallRyeThreadContext unwrapThreadContext(ThreadContext executor) {
        if (executor instanceof ClientProxy) {
            return (SmallRyeThreadContext) ((ClientProxy) executor).arc_contextualInstance();
        } else {
            throw new IllegalStateException("Injected proxies are expected to be instance of ClientProxy");
        }
    }

    public static Set<String> providersToStringSet(Set<ThreadContextProvider> providers) {
        Set<String> result = new HashSet<>();
        for (ThreadContextProvider provider : providers) {
            result.add(provider.getThreadContextType());
        }
        return result;
    }
}
