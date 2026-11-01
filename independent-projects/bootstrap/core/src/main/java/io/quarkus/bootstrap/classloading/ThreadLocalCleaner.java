package io.quarkus.bootstrap.classloading;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.UndeclaredThrowableException;

import org.jboss.logging.Logger;

/**
 * Removes ThreadLocal entries whose key or value was loaded by a specific ClassLoader.
 * <p>
 * When a ClassLoader is closed during test or dev-mode restarts, ThreadLocal entries
 * on surviving threads (e.g. the main/Surefire thread) can retain classes loaded by
 * that ClassLoader, preventing it from being garbage collected. This causes metaspace
 * leaks.
 * <p>
 * This cleaner walks each live thread's ThreadLocalMap and removes entries whose
 * ThreadLocal key or stored value was loaded by the closing ClassLoader (or any
 * child of it). This is the same approach used by Tomcat's
 * {@code WebappClassLoaderBase.checkThreadLocalsForLeaks()}.
 */
final class ThreadLocalCleaner {

    private static final Logger log = Logger.getLogger(ThreadLocalCleaner.class);

    private static final MethodHandle GET_THREAD_LOCALS;
    private static final MethodHandle GET_INHERITABLE_THREAD_LOCALS;
    private static final MethodHandle GET_TABLE;
    private static final MethodHandle GET_ENTRY_VALUE;
    private static final MethodHandle SET_ENTRY_VALUE;
    private static final boolean AVAILABLE;

    static {
        MethodHandle getThreadLocals = null;
        MethodHandle getInheritableThreadLocals = null;
        MethodHandle getTable = null;
        MethodHandle getEntryValue = null;
        MethodHandle setEntryValue = null;
        boolean available = false;
        try {
            MethodHandles.Lookup threadLookup = MethodHandles.privateLookupIn(Thread.class, MethodHandles.lookup());

            getThreadLocals = threadLookup
                    .unreflectVarHandle(Thread.class.getDeclaredField("threadLocals"))
                    .toMethodHandle(VarHandle.AccessMode.GET)
                    .asType(MethodType.methodType(Object.class, Thread.class));
            getInheritableThreadLocals = threadLookup
                    .unreflectVarHandle(Thread.class.getDeclaredField("inheritableThreadLocals"))
                    .toMethodHandle(VarHandle.AccessMode.GET)
                    .asType(MethodType.methodType(Object.class, Thread.class));

            Class<?> threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            MethodHandles.Lookup mapLookup = MethodHandles.privateLookupIn(threadLocalMapClass, MethodHandles.lookup());

            getTable = mapLookup
                    .unreflectVarHandle(threadLocalMapClass.getDeclaredField("table"))
                    .toMethodHandle(VarHandle.AccessMode.GET)
                    .asType(MethodType.methodType(Object[].class, Object.class));

            Class<?> entryClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry");
            MethodHandles.Lookup entryLookup = MethodHandles.privateLookupIn(entryClass, MethodHandles.lookup());

            VarHandle entryValueHandle = entryLookup.unreflectVarHandle(entryClass.getDeclaredField("value"));
            getEntryValue = entryValueHandle
                    .toMethodHandle(VarHandle.AccessMode.GET)
                    .asType(MethodType.methodType(Object.class, Object.class));
            setEntryValue = entryValueHandle
                    .toMethodHandle(VarHandle.AccessMode.SET)
                    .asType(MethodType.methodType(void.class, Object.class, Object.class));

            available = true;
        } catch (IllegalAccessException e) {
            Module myModule = ThreadLocalCleaner.class.getModule();
            String myName = myModule.isNamed() ? myModule.getName() : "ALL-UNNAMED";
            log.info("ThreadLocal cleanup not available; to enable it, use this JVM option: "
                    + "--add-opens java.base/java.lang=" + myName);
        } catch (Throwable t) {
            log.info("ThreadLocal cleanup not available", t);
        }
        GET_THREAD_LOCALS = getThreadLocals;
        GET_INHERITABLE_THREAD_LOCALS = getInheritableThreadLocals;
        GET_TABLE = getTable;
        GET_ENTRY_VALUE = getEntryValue;
        SET_ENTRY_VALUE = setEntryValue;
        AVAILABLE = available;
    }

    static void cleanLoadedBy(ClassLoader closingCL) {
        if (!AVAILABLE) {
            return;
        }
        int cleaned = 0;
        boolean logDetails = log.isInfoEnabled();
        StringBuilder details = logDetails ? new StringBuilder() : null;
        Thread[] threads = getThreads();
        for (Thread thread : threads) {
            if (thread == null) {
                continue;
            }
            try {
                cleaned += cleanMap(GET_THREAD_LOCALS.invokeExact(thread), closingCL, thread, details);
                cleaned += cleanMap(GET_INHERITABLE_THREAD_LOCALS.invokeExact(thread), closingCL, thread, details);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
        if (cleaned > 0) {
            log.infof("Cleaned %d ThreadLocal entries loaded by %s:%s", cleaned, closingCL, details);
        }
    }

    private static int cleanMap(Object threadLocalMap, ClassLoader closingCL,
            Thread thread, StringBuilder details) throws Throwable {
        if (threadLocalMap == null) {
            return 0;
        }
        int cleaned = 0;
        Object[] table = (Object[]) GET_TABLE.invokeExact(threadLocalMap);
        if (table == null) {
            return 0;
        }
        for (Object entry : table) {
            if (entry == null) {
                continue;
            }
            Object value = GET_ENTRY_VALUE.invokeExact(entry);
            boolean shouldClean = false;

            if (value != null && isLoadedBy(value.getClass().getClassLoader(), closingCL)) {
                shouldClean = true;
            }

            @SuppressWarnings("unchecked")
            ThreadLocal<?> key = ((WeakReference<ThreadLocal<?>>) entry).get();

            if (!shouldClean) {
                if (key != null && isLoadedBy(key.getClass().getClassLoader(), closingCL)) {
                    shouldClean = true;
                }
            }

            if (shouldClean) {
                if (details != null) {
                    details.append("\n  - thread=").append(thread.getName())
                            .append(", key=").append(key != null ? key.getClass().getName() : "<gc'd>")
                            .append(", value=").append(value != null ? value.getClass().getName() : "null");
                }
                SET_ENTRY_VALUE.invokeExact(entry, (Object) null);
                ((Reference<?>) entry).clear();
                cleaned++;
            }
        }
        return cleaned;
    }

    private static boolean isLoadedBy(ClassLoader cl, ClassLoader target) {
        while (cl != null) {
            if (cl == target) {
                return true;
            }
            cl = cl.getParent();
        }
        return false;
    }

    private static Thread[] getThreads() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (group.getParent() != null) {
            group = group.getParent();
        }
        int estimate = group.activeCount() + 16;
        Thread[] threads = new Thread[estimate];
        while (group.enumerate(threads, true) == threads.length) {
            threads = new Thread[threads.length * 2];
        }
        return threads;
    }

    private ThreadLocalCleaner() {
    }
}
