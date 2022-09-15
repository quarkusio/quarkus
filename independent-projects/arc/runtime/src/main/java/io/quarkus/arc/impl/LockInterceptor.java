package io.quarkus.arc.impl;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.Lock;
import io.quarkus.arc.LockException;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Lock
@Interceptor
@Priority(PLATFORM_BEFORE)
public class LockInterceptor {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @AroundInvoke
    Object lock(InvocationContext ctx) throws Exception {
        Lock lock = getLock(ctx);
        switch (lock.value()) {
            case WRITE:
                return writeLock(lock, ctx);
            case READ:
                return readLock(lock, ctx);
            case NONE:
                return ctx.proceed();
        }
        throw new LockException("Unsupported @Lock type found on business method " + ctx.getMethod());
    }

    private Object writeLock(Lock lock, InvocationContext ctx) throws Exception {
        boolean locked = false;
        long time = lock.time();
        try {
            if (time > 0) {
                locked = readWriteLock.writeLock().tryLock(time, lock.unit());
                if (!locked) {
                    throw new LockException("Write lock not acquired in " + lock.unit().toMillis(time) + " ms");
                }
            } else {
                readWriteLock.writeLock().lock();
                locked = true;
            }
            return ctx.proceed();
        } finally {
            if (locked) {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    private Object readLock(Lock lock, InvocationContext ctx) throws Exception {
        boolean locked = false;
        long time = lock.time();
        try {
            if (time > 0) {
                locked = readWriteLock.readLock().tryLock(time, lock.unit());
                if (!locked) {
                    throw new LockException("Read lock not acquired in " + lock.unit().toMillis(time) + " ms");
                }
            } else {
                readWriteLock.readLock().lock();
                locked = true;
            }
            return ctx.proceed();
        } finally {
            if (locked) {
                readWriteLock.readLock().unlock();
            }
        }
    }

    @SuppressWarnings("unchecked")
    Lock getLock(InvocationContext ctx) {
        Set<Annotation> bindings = (Set<Annotation>) ctx.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
        for (Annotation annotation : bindings) {
            if (annotation.annotationType().equals(Lock.class)) {
                return (Lock) annotation;
            }
        }
        // This should never happen
        throw new LockException("@Lock binding not found on business method " + ctx.getMethod());
    }

}
