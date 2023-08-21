package io.quarkus.arc.impl;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.Lock;
import io.quarkus.arc.LockException;

@Lock
@Interceptor
@Priority(PLATFORM_BEFORE)
public class LockInterceptor {

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    // This lock is used exclusively to synchronize the block where we release all read locks and aquire the write lock
    private final ReentrantLock rl = new ReentrantLock();

    @AroundInvoke
    Object lock(ArcInvocationContext ctx) throws Exception {
        Lock lock = getLock(ctx);
        switch (lock.value()) {
            case WRITE:
                return writeLock(lock, ctx);
            case READ:
                return readLock(lock, ctx);
            case NONE:
                return ctx.proceed();
            default:
                throw new LockException("Unsupported @Lock type found on business method " + ctx.getMethod());
        }
    }

    private Object writeLock(Lock lock, InvocationContext ctx) throws Exception {
        long time = lock.time();
        int readHoldCount = rwl.getReadHoldCount();
        boolean locked = false;

        try {
            if (readHoldCount > 0) {
                rl.lock();
            }
            try {
                if (readHoldCount > 0) {
                    // Release all read locks hold by the current thread before acquiring the write lock
                    for (int i = 0; i < readHoldCount; i++) {
                        rwl.readLock().unlock();
                    }
                }
                if (time > 0) {
                    locked = rwl.writeLock().tryLock(time, lock.unit());
                    if (!locked) {
                        throw new LockException("Write lock not acquired in " + lock.unit().toMillis(time) + " ms");
                    }
                } else {
                    rwl.writeLock().lock();
                    locked = true;
                }
            } finally {
                if (readHoldCount > 0) {
                    rl.unlock();
                }
            }
            return ctx.proceed();
        } finally {
            if (locked) {
                if (readHoldCount > 0) {
                    // Re-aqcquire the read locks
                    for (int i = 0; i < readHoldCount; i++) {
                        rwl.readLock().lock();
                    }
                }
                rwl.writeLock().unlock();
            }
        }
    }

    private Object readLock(Lock lock, InvocationContext ctx) throws Exception {
        boolean locked = false;
        long time = lock.time();
        try {
            if (time > 0) {
                locked = rwl.readLock().tryLock(time, lock.unit());
                if (!locked) {
                    throw new LockException("Read lock not acquired in " + lock.unit().toMillis(time) + " ms");
                }
            } else {
                rwl.readLock().lock();
                locked = true;
            }
            return ctx.proceed();
        } finally {
            if (locked) {
                rwl.readLock().unlock();
            }
        }
    }

    Lock getLock(ArcInvocationContext ctx) {
        Lock lock = ctx.findIterceptorBinding(Lock.class);
        if (lock == null) {
            // This should never happen
            throw new LockException("@Lock binding not found on business method " + ctx.getMethod());
        }
        return lock;
    }

}
