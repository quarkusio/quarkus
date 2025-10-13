package io.quarkus.virtual.graphql;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

import io.smallrye.common.annotation.RunOnVirtualThread;

@GraphQLApi
public class RunOnVirtualThreadObjectTestThreadResource {

    // Return type Object with @RunOnVirtualThread
    @Query
    @RunOnVirtualThread
    public TestThread annotatedRunOnVirtualThreadObject() {
        sleep();
        return getTestThread();
    }

    // Return type Object with @RunOnVirtualThread
    @Mutation
    @RunOnVirtualThread
    public TestThread annotatedRunOnVirtualThreadMutationObject(String test) {
        sleep();
        return getTestThread();
    }

    @Query
    @RunOnVirtualThread
    public TestThread pinThread() {
        // Synchronize on an object to cause thread pinning
        Object lock = new Object();
        synchronized (lock) {
            sleep();
        }
        return getTestThread();
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private TestThread getTestThread() {
        Thread t = Thread.currentThread();
        long id = t.getId();
        String name = t.getName();
        int priority = t.getPriority();
        String state = t.getState().name();
        String group = t.getThreadGroup().getName();
        return new TestThread(id, name, priority, state, group);
    }
}
