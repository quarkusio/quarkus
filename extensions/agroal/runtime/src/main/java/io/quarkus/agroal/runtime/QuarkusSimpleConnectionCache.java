package io.quarkus.agroal.runtime;

import org.jboss.threads.JBossThread;

import io.agroal.api.cache.Acquirable;
import io.agroal.api.cache.ConnectionCache;

class QuarkusSimpleConnectionCache implements ConnectionCache {

    volatile ThreadLocal<Acquirable> connectionCache = new ThreadLocal<>();

    @Override
    public Acquirable get() {
        Thread thread = Thread.currentThread();
        if (thread instanceof JBossThread) {
            //we only want to cache on threads that we control the lifecycle
            //which are the vert.x and potentially jboss threads
            //JBossThread still works with FastThreadLocal, it is just slower, and for most apps
            //this will not be used anyway, as we use VertThread pretty much everywhere if
            //Vert.x is present
            Acquirable acquirable = connectionCache.get();
            return acquirable != null && acquirable.acquire() ? acquirable : null;
        }
        return null;
    }

    @Override
    public void put(Acquirable acquirable) {
        Thread thread = Thread.currentThread();
        if (thread instanceof JBossThread) {
            connectionCache.set(acquirable);
        }
    }

    @Override
    public void reset() {
        connectionCache = new ThreadLocal<>();
    }
}
