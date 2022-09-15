package io.quarkus.arc.runtime.devconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jakarta.inject.Singleton;

@Singleton
public class InvocationsMonitor {

    private static final int DEFAULT_LIMIT = 1000;

    private final List<Invocation> invocations = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean filterOutQuarkusBeans = true;

    void addInvocation(Invocation invocation) {
        if (invocations.size() > DEFAULT_LIMIT) {
            // Remove some old data if the limit is exceeded
            synchronized (invocations) {
                if (invocations.size() > DEFAULT_LIMIT) {
                    invocations.subList(0, DEFAULT_LIMIT / 2).clear();
                }
            }
        }
        invocations.add(invocation);
    }

    public List<Invocation> getLastInvocations() {
        List<Invocation> result = new ArrayList<>(invocations);
        if (filterOutQuarkusBeans) {
            for (Iterator<Invocation> it = result.iterator(); it.hasNext();) {
                Invocation invocation = it.next();
                if (invocation.getInterceptedBean().getBeanClass().getName().startsWith("io.quarkus")) {
                    it.remove();
                }
            }
        }
        Collections.reverse(result);
        return result;
    }

    public void clear() {
        invocations.clear();
    }

    public boolean isFilterOutQuarkusBeans() {
        return filterOutQuarkusBeans;
    }

    public void toggleFilterOutQuarkusBeans() {
        // This is not thread-safe but we don't expect concurrent actions from dev ui
        filterOutQuarkusBeans = !filterOutQuarkusBeans;
    }

}
