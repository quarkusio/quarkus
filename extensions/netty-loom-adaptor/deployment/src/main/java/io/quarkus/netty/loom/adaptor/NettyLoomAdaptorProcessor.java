package io.quarkus.netty.loom.adaptor;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LCMP;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import java.io.IOException;
import java.util.function.BiFunction;

import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.netty.deployment.MinNettyAllocatorMaxOrderBuildItem;
import io.smallrye.common.annotation.RunOnVirtualThread;

public class NettyLoomAdaptorProcessor {
    static Logger LOG = Logger.getLogger(NettyLoomAdaptorProcessor.class);

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem("netty-Loom-adaptor");
    }

    /**
     * This extension is designed to stop using Netty's {@link io.netty.buffer.PooledByteBufAllocator.PoolThreadLocalCache
     * PoolThreadLocalCache}, extending {@link io.netty.util.concurrent.FastThreadLocal FastThreadLocal} in the
     * {@link io.netty.buffer.PooledByteBufAllocator#newDirectBuffer(int, int)} newDirectBuffer(int,int)} method and to replace
     * them with a {@link java.util.concurrent.ConcurrentHashMap ConcurrentHashMap} using the carrier thread's name as a key.
     *
     * we want to instrument the source ({@link io.netty.buffer.PooledByteBufAllocator#newDirectBuffer(int, int)}
     * newDirectBuffer(int,int)} to get:
     *
     * protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity) {
     * boolean isVirtual = false;
     * PoolThreadCache cache=null;
     * if(canUseVirtual){
     * try {
     * isVirtual = (boolean) isVirtualMethod.invoke(Thread.currentThread());
     * } catch (IllegalAccessException | InvocationTargetException e) {
     * System.err.println(e);
     * }
     * if(isVirtual){
     * cache = createCache(initialCapacity, maxCapacity);
     * }
     * }
     * if(cache == null){
     * cache = threadCache.get();
     * }
     * PoolArena<ByteBuffer> directArena = cache.directArena;
     *
     * final ByteBuf buf;
     * if (directArena != null) {
     * buf = directArena.allocate(cache, initialCapacity, maxCapacity);
     * } else {
     * buf = PlatformDependent.hasUnsafe() ?
     * UnsafeByteBufUtil.newUnsafeDirectByteBuf(this, initialCapacity, maxCapacity) :
     * new UnpooledDirectByteBuf(this, initialCapacity, maxCapacity);
     * }
     *
     * return toLeakAwareBuffer(buf);
     * }
     *
     * private PoolThreadCache createCache(int initialCapacity, int maxCapacity){
     * PoolThreadCache cache;
     * Thread currentCarrierThread;
     * try {
     * currentCarrierThread = (Thread) getCurrentCarrierMethod.invoke(null);
     * } catch (InvocationTargetException | IllegalAccessException e) {
     * System.out.println(e);
     * return null;
     * }
     * if(threadCaches.containsKey(currentCarrierThread)){
     * return threadCaches.get(currentCarrierThread);
     * }else{
     * PoolArena<byte[]> heapArena = leastUsedArena(heapArenas);
     * PoolArena<ByteBuffer> directArena = leastUsedArena(directArenas);
     *
     * cache = new PoolThreadCache(
     * heapArena, directArena, smallCacheSize, normalCacheSize,
     * DEFAULT_MAX_CACHED_BUFFER_CAPACITY, DEFAULT_CACHE_TRIM_INTERVAL);
     * threadCaches.put(currentCarrierThread, cache);
     * if (DEFAULT_CACHE_TRIM_INTERVAL_MILLIS > 0) {
     * EventExecutor executor = ThreadExecutorMap.currentExecutor();
     * if (executor != null) {
     * executor.scheduleAtFixedRate(trimTask, DEFAULT_CACHE_TRIM_INTERVAL_MILLIS,
     * DEFAULT_CACHE_TRIM_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
     * }
     * }
     * }
     * return cache;
     * }
     *
     */
    @Produce(EmptyBuildItem.class)
    @Consume(MinNettyAllocatorMaxOrderBuildItem.class)
    @BuildStep
    void adaptNetty(CombinedIndexBuildItem combinedIndexBuildItem, BuildProducer<BytecodeTransformerBuildItem> producer)
            throws IOException {
        var runOnVirtualThreadAnnotations = combinedIndexBuildItem.getComputingIndex()
                .getAnnotations(DotName.createSimple(RunOnVirtualThread.class.getName())).size();
        if (runOnVirtualThreadAnnotations == 0) {
            return;
        }
        var klass = "io.netty.buffer.PooledByteBufAllocator";

        producer.produce(new BytecodeTransformerBuildItem(klass, new BiFunction<String, ClassVisitor, ClassVisitor>() {
            @Override
            public ClassVisitor apply(String cls, ClassVisitor classVisitor) {
                return new NettyCurrentAdaptor(ASM9, classVisitor);
            }

        }));
    }

    private class NettyCurrentAdaptor extends ClassVisitor {
        public NettyCurrentAdaptor(int version, ClassVisitor cv) {
            super(version, cv);
            LOG.info("Adapting Netty for Loom...");
        }

        @Override
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String descriptor,
                final String signature,
                final String[] exceptions) {
            if (cv != null) {
                MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals("<clinit>")) {
                    // we need to augment the <clinit> method to assigned the different static fields we added to the
                    // {@link io.netty.buffer.PooledByteBufAllocator PooledByteBufAllocator} class
                    mv = new MethodVisitor(Gizmo.ASM_API_VERSION, mv) {
                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == RETURN) {
                                Label L0 = new Label();
                                Label L1 = new Label();
                                Label L2 = new Label();

                                Label LthreadCaches = new Label();
                                Label lcanUseVirtual = new Label();

                                // set canUseVirtual to true
                                mv.visitLabel(lcanUseVirtual);
                                mv.visitInsn(ICONST_1);
                                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf",
                                        "(Z)Ljava/lang/Boolean;", false);
                                mv.visitFieldInsn(PUTSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                                        "canUseVirtual", "Ljava/lang/Boolean;");

                                // fetch the currentCarrierThread method and put it inside the getCurrentCarrierMethod field
                                // to avoid having to fetch it everytime we need to invoke it
                                mv.visitLabel(L0);
                                mv.visitLdcInsn("java.lang.Thread");
                                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName",
                                        "(Ljava/lang/String;)Ljava/lang/Class;", false);
                                mv.visitLdcInsn("currentCarrierThread");
                                mv.visitInsn(ICONST_0);
                                mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
                                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod",
                                        "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                                        false);
                                mv.visitFieldInsn(PUTSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                                        "getCurrentCarrierMethod", "Ljava/lang/reflect/Method;");
                                mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                                        "getCurrentCarrierMethod", "Ljava/lang/reflect/Method;");
                                mv.visitInsn(ICONST_1);
                                // make it accessible
                                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible",
                                        "(Z)V", false);

                                // fetch the isVirtual method and put it inside the isVirtualMethod field to avoid
                                // having to fetch it everytime we need to invoke it
                                mv.visitLdcInsn("java.lang.Thread");
                                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName",
                                        "(Ljava/lang/String;)Ljava/lang/Class;", false);
                                mv.visitLdcInsn("isVirtual");
                                mv.visitInsn(ICONST_0);
                                mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
                                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod",
                                        "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                                        false);
                                mv.visitFieldInsn(PUTSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                                        "isVirtualMethod", "Ljava/lang/reflect/Method;");

                                mv.visitLabel(L1);
                                mv.visitJumpInsn(GOTO, LthreadCaches);

                                // catch block of reflective calls to fetch isVirtual and currentCarrierThread.
                                // we set the canUseVirtual field to false if we enter the catch block:
                                // if these methods can't be found the jdk is not quarkus-loom compliant
                                mv.visitLabel(L2);
                                mv.visitVarInsn(ASTORE, 0);
                                mv.visitInsn(ICONST_0);
                                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf",
                                        "(Z)Ljava/lang/Boolean;", false);
                                mv.visitFieldInsn(PUTSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                                        "canUseVirtual", "Ljava/lang/Boolean;");

                                // create the static concurrentHashMap that will be populated
                                mv.visitLabel(LthreadCaches);
                                mv.visitTypeInsn(NEW, "java/util/concurrent/ConcurrentHashMap");
                                mv.visitInsn(DUP);
                                mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/ConcurrentHashMap",
                                        "<init>", "()V", false);
                                mv.visitFieldInsn(PUTSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                                        "threadCaches", "Ljava/util/concurrent/ConcurrentHashMap;");

                                mv.visitTryCatchBlock(L0, L1, L2, "java/lang/NoSuchMethodException");
                                mv.visitTryCatchBlock(L0, L1, L2, "java/lang/ClassNotFoundException");
                            }
                            super.visitInsn(opcode);
                        }

                    };
                    mv.visitMaxs(3, 3);
                    return mv;
                }
                if (name.equals("newDirectBuffer")) {
                    // this is the actual method we want to modify
                    mv = new CurrentThreadMethodAdaptor(Gizmo.ASM_API_VERSION, mv);
                    mv.visitMaxs(4, 4);
                    return mv;
                }
                return mv;
            }
            return null;
        }

        /**
         * this method contains logic that was previously in
         * {@link io.netty.buffer.PooledByteBufAllocator#newDirectBuffer(int, int)} newDirectBuffer(int, int)
         * The FastThreadLocals are used to store thread cache, they are hence created with an initial value that needs a
         * {@link io.netty.buffer.PoolArena}, this is
         */
        public void createLeastUsedArenaMethod() {
            var L0 = new Label();
            var L1 = new Label();
            var L2 = new Label();
            var L3 = new Label();
            var L4 = new Label();
            var L5 = new Label();
            var L6 = new Label();
            var L7 = new Label();
            var L8 = new Label();
            var L9 = new Label();
            var L10 = new Label();
            var mv = cv.visitMethod(ACC_PRIVATE, "leastUsedArena",
                    "([Lio/netty/buffer/PoolArena;)Lio/netty/buffer/PoolArena;", null, null);
            mv.visitLabel(L0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitJumpInsn(IFNULL, L1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARRAYLENGTH);
            mv.visitJumpInsn(IFNE, L2);

            mv.visitLabel(L1);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);

            mv.visitLabel(L2);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ICONST_0);
            mv.visitInsn(AALOAD);
            mv.visitVarInsn(ASTORE, 2);

            mv.visitLabel(L3);
            mv.visitInsn(ICONST_1);
            mv.visitVarInsn(ISTORE, 3);

            mv.visitLabel(L4);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARRAYLENGTH);
            mv.visitJumpInsn(IF_ICMPGE, L5);

            mv.visitLabel(L5);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitInsn(AALOAD);
            mv.visitVarInsn(ASTORE, 4);

            mv.visitLabel(L7);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitFieldInsn(GETFIELD, "io/netty/buffer/PoolArena", "numThreadCaches",
                    "Ljava/util/concurrent/atomic/AtomicInteger;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get",
                    "()I", false);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitFieldInsn(GETFIELD, "io/netty/buffer/PoolArena", "numThreadCaches",
                    "Ljava/util/concurrent/atomic/AtomicInteger;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get",
                    "()I", false);
            mv.visitJumpInsn(IF_ICMPGE, L8);

            mv.visitLabel(L9);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitVarInsn(ASTORE, 2);

            mv.visitLabel(L8);
            mv.visitIincInsn(3, 1);
            mv.visitJumpInsn(GOTO, L4);

            mv.visitLabel(L5);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ARETURN);

            mv.visitLabel(L10);
            mv.visitLocalVariable("arena", "Lio/netty/buffer/PoolArena;",
                    "Lio/netty/buffer/PoolArena<TT;>;", L7, L8, 4);
            mv.visitLocalVariable("i", "I", null, L4, L5, 3);
            mv.visitLocalVariable("this", "Lio/netty/buffer/PooledByteBufAllocator;",
                    null, L0, L10, 0);
            mv.visitLocalVariable("arenas", "[Lio/netty/buffer/PoolArena;",
                    "[Lio/netty/buffer/PoolArena<TT;>;", L0, L10, 1);
            mv.visitLocalVariable("minArena", "Lio/netty/buffer/PoolArena;",
                    "Lio/netty/buffer/PoolArena<TT;>;", L3, L10, 2);
            mv.visitMaxs(2, 5);
        }

        /**
         * this method contains logic that was previously in
         * {@link io.netty.buffer.PooledByteBufAllocator#newDirectBuffer(int, int)} newDirectBuffer(int, int)
         * it was a method of {@link io.netty.buffer.PooledByteBufAllocator.PoolThreadLocalCache PoolThreadLocalCache},
         * we need to reimplement it outside of this subclass that we don't use anymore
         */
        public void createCacheMethod() {
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label LError = new Label();
            Label LStart = new Label();
            Label LEnd = new Label();
            Label testHashMap = new Label();
            Label LKeyIn = new Label();
            Label LKeyOut = new Label();
            Label L14 = new Label();
            //needs to be private
            var mv = cv.visitMethod(ACC_PRIVATE, "createCache", "(II)Lio/netty/buffer/PoolThreadCache;", null, null);
            mv.visitLabel(LStart);
            //set currentCarrier to the currentThread
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread",
                    "()Ljava/lang/Thread;", false);
            mv.visitTypeInsn(CHECKCAST, "java/lang/Thread");
            mv.visitVarInsn(ASTORE, 5);

            //we try to access the currentCarrierThread method
            mv.visitLabel(L0);
            mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                    "getCurrentCarrierMethod", "Ljava/lang/reflect/Method;");
            //we store the result in method
            mv.visitTypeInsn(CHECKCAST, "java/lang/reflect/Method");
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread",
                    "()Ljava/lang/Thread;", false);
            mv.visitInsn(ICONST_0);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
                    "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "java/lang/Thread");
            mv.visitVarInsn(ASTORE, 5);

            //we finished to try to access currentCarrierThread and it went fine, we jump to the next thing to do
            mv.visitLabel(L1);
            mv.visitJumpInsn(GOTO, testHashMap);

            //to handle the exception we merely store it in 7
            mv.visitLabel(L2);
            mv.visitLabel(LError);
            mv.visitVarInsn(ASTORE, 6);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);

            //we try to access the currentHashmap
            mv.visitLabel(testHashMap);
            mv.visitInsn(ACONST_NULL);
            mv.visitTypeInsn(CHECKCAST, "io/netty/buffer/PoolThreadCache");
            mv.visitVarInsn(ASTORE, 3);
            mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator", "threadCaches",
                    "Ljava/util/concurrent/ConcurrentHashMap;");
            //we store the testHashMap
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/ConcurrentHashMap");
            mv.visitVarInsn(ASTORE, 7);
            mv.visitVarInsn(ALOAD, 7);
            mv.visitVarInsn(ALOAD, 5);
            //... currentCarrierThread.getName()
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap", "containsKey",
                    "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(IFEQ, LKeyOut);

            //the carrier name is already a key in the concurrentHashMap
            mv.visitLabel(LKeyIn);
            mv.visitVarInsn(ALOAD, 7);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap", "get",
                    "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "io/netty/buffer/PoolThreadCache");
            mv.visitInsn(ARETURN);

            //the carrier name is not already a key in the concurrentHashMap
            mv.visitLabel(LKeyOut);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "io/netty/buffer/PooledByteBufAllocator", "heapArenas",
                    "[Lio/netty/buffer/PoolArena;");
            mv.visitMethodInsn(INVOKESPECIAL, "io/netty/buffer/PooledByteBufAllocator", "leastUsedArena",
                    "([Lio/netty/buffer/PoolArena;)Lio/netty/buffer/PoolArena;", false);
            mv.visitVarInsn(ASTORE, 6);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "io/netty/buffer/PooledByteBufAllocator", "directArenas",
                    "[Lio/netty/buffer/PoolArena;");
            mv.visitMethodInsn(INVOKESPECIAL, "io/netty/buffer/PooledByteBufAllocator", "leastUsedArena",
                    "([Lio/netty/buffer/PoolArena;)Lio/netty/buffer/PoolArena;", false);
            mv.visitVarInsn(ASTORE, 9);
            mv.visitTypeInsn(NEW, "io/netty/buffer/PoolThreadCache");

            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 6);
            mv.visitVarInsn(ALOAD, 9);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "io/netty/buffer/PooledByteBufAllocator", "smallCacheSize",
                    "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "io/netty/buffer/PooledByteBufAllocator", "normalCacheSize",
                    "I");
            mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                    "DEFAULT_MAX_CACHED_BUFFER_CAPACITY", "I");
            mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                    "DEFAULT_CACHE_TRIM_INTERVAL", "I");
            mv.visitMethodInsn(INVOKESPECIAL, "io/netty/buffer/PoolThreadCache", "<init>",
                    "(Lio/netty/buffer/PoolArena;Lio/netty/buffer/PoolArena;IIII)V", false);
            mv.visitVarInsn(ASTORE, 3);
            mv.visitVarInsn(ALOAD, 7);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap", "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);

            mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                    "DEFAULT_CACHE_TRIM_INTERVAL_MILLIS", "J");
            mv.visitInsn(LCONST_0);
            mv.visitInsn(LCMP);
            mv.visitJumpInsn(IFLE, L14);

            mv.visitLabel(new Label());
            mv.visitMethodInsn(INVOKESTATIC, "io/netty/util/internal/ThreadExecutorMap", "currentExecutor",
                    "()Lio/netty/util/concurrent/EventExecutor;", false);
            mv.visitVarInsn(ASTORE, 10);
            mv.visitVarInsn(ALOAD, 10);
            mv.visitJumpInsn(IFNULL, L14);
            mv.visitVarInsn(ALOAD, 10);
            mv.visitVarInsn(ALOAD, 0);

            mv.visitFieldInsn(GETFIELD, "io/netty/buffer/PooledByteBufAllocator", "trimTask",
                    "Ljava/lang/Runnable;");
            mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                    "DEFAULT_CACHE_TRIM_INTERVAL_MILLIS", "J");
            mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator",
                    "DEFAULT_CACHE_TRIM_INTERVAL_MILLIS", "J");
            mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator", "MILLISECONDS",
                    "Ljava/util/concurrent/TimeUnit;");
            mv.visitMethodInsn(INVOKEINTERFACE, "io/netty/util/concurrent/EventExecutor",
                    "scheduleAtFixedRate",
                    "(Ljava/lang/Runnable;JJLjava/util/concurrent/TimeUnit;)Lio/netty/util/concurrent/ScheduledFuture;",
                    true);
            mv.visitInsn(POP);

            mv.visitLabel(L14);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitInsn(ARETURN);
            mv.visitLabel(LEnd);

            mv.visitTryCatchBlock(L0, L1, L2, "java/lang/NoSuchMethodException");
            mv.visitTryCatchBlock(L0, L1, L2, "java/lang/ClassNotFoundException");
            mv.visitTryCatchBlock(L0, L1, L2, "java/lang/reflect/InvocationTargetException");
            mv.visitTryCatchBlock(L0, L1, L2, "java/lang/IllegalAccessException");

            mv.visitLocalVariable("cache", "Lio/netty/buffer/PoolThreadCache;", null, testHashMap, LEnd, 3);
            mv.visitLocalVariable("this", "Lio/netty/buffer/PooledByteBufAllocator;", null, LStart, LEnd, 0);
            mv.visitLocalVariable("initialCapacity", "I", null, LStart, LEnd, 1);
            mv.visitLocalVariable("maxCapacity", "I", null, LStart, LEnd, 2);
            mv.visitLocalVariable("method", "Ljava/lang/reflect/Method;", null, L0, LEnd, 4);
            mv.visitLocalVariable("currentCarrierThread", "Ljava/lang/Thread;", null, LStart, LEnd, 5);
            mv.visitLocalVariable("e", "Ljava/lang/ReflectiveOperationException;", null,
                    LError, testHashMap, 6);
            mv.visitLocalVariable("lthreadCaches", "Ljava/util/concurrent/ConcurrentHashMap;",
                    "Ljava/util/concurrent/ConcurrentHashMap<Ljava/lang/Thread;Lio/netty/buffer/PoolThreadCache;>;",
                    testHashMap, LEnd, 7);
            mv.visitLocalVariable("heapArena", "Lio/netty/buffer/PoolArena;",
                    "Lio/netty/buffer/PoolArena<[B>;", LKeyOut, LEnd, 6);
            mv.visitLocalVariable("directArena", "Lio/netty/buffer/PoolArena;",
                    "Lio/netty/buffer/PoolArena<[B>;", LKeyOut, LEnd, 9);

            mv.visitMaxs(5, 10);
        }

        @Override
        public void visitEnd() {
            cv.visitField(ACC_STATIC | ACC_PRIVATE, "isVirtualMethod",
                    "Ljava/lang/reflect/Method;",
                    null,
                    null);
            cv.visitField(ACC_STATIC | ACC_PRIVATE, "getCurrentCarrierMethod",
                    "Ljava/lang/reflect/Method;",
                    null,
                    null);
            cv.visitField(ACC_STATIC | ACC_PRIVATE, "canUseVirtual",
                    "Ljava/lang/Boolean;",
                    null,
                    null);
            cv.visitField(ACC_STATIC | ACC_PRIVATE, "threadCaches",
                    "Ljava/util/concurrent/ConcurrentHashMap;",
                    "Ljava/util/concurrent/ConcurrentHashMapConcurrentHashMap<Ljava/lang/Thread;Lio/netty/buffer/PoolThreadCache;>;",
                    null);

            if (cv != null) {
                createLeastUsedArenaMethod();
                createCacheMethod();
                cv.visitEnd();
            }
        }

    }

    private class NettyCurrentAdaptorPrinter extends NettyCurrentAdaptor {
        public NettyCurrentAdaptorPrinter(int version, ClassVisitor cv) {
            super(version, cv);
        }

        @Override
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String descriptor,
                final String signature,
                final String[] exceptions) {
            if (cv != null) {
                if (name.equals("newDirectBuffer")) {
                    MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
                    mv = new CurrentThreadMethodAdaptor(Gizmo.ASM_API_VERSION, mv);
                    mv.visitMaxs(4, 4);
                    return mv;
                }
                return null;
            }
            return null;
        }
    }

    private static void addPrintInsn(String msg, MethodVisitor mv) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(msg);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private static void addPrintVar(int var, String type, int instruction, MethodVisitor mv) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        mv.visitVarInsn(instruction, var);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(" + type + ")V", false);
    }

    private class CurrentThreadMethodAdaptor extends MethodVisitor {
        boolean firstReturn = true;
        MethodVisitor mv;

        public CurrentThreadMethodAdaptor(int api, MethodVisitor methodVisitor) {
            super(api, null);
            mv = methodVisitor;
        }

        @Override
        public void visitCode() {
            mv.visitCode();
            firstReturn = false;
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L16 = new Label();
            Label L18 = new Label();
            Label LNullDirectArena = new Label();

            Label LStart = new Label();
            Label LTest = new Label();

            Label lVirtual = new Label();
            Label lTestCache = new Label();
            Label lAfter = new Label();
            Label LgotCache = new Label();
            Label LReturn = new Label();
            Label LEnd = new Label();

            //...Thread.class.getMethod("isVirtual")...
            mv.visitLabel(LStart);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 3);
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, 4);
            mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator", "canUseVirtual",
                    "Ljava/lang/Boolean;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue",
                    "()Z", false);
            mv.visitJumpInsn(IFEQ, lTestCache);

            mv.visitLabel(L0);
            mv.visitFieldInsn(GETSTATIC, "io/netty/buffer/PooledByteBufAllocator", "isVirtualMethod",
                    "Ljava/lang/reflect/Method;");
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread",
                    "()Ljava/lang/Thread;", false);
            mv.visitInsn(ICONST_0);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
                    "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);

            mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue",
                    "()Z", false);
            mv.visitVarInsn(ISTORE, 3);

            mv.visitLabel(L1);
            mv.visitJumpInsn(GOTO, LTest);

            mv.visitLabel(L2);
            mv.visitVarInsn(ASTORE, 4);
            addPrintInsn("error in newDirectBuffer : ", mv);
            addPrintVar(4, "Ljava/lang/Object;", ALOAD, mv);

            mv.visitLabel(LTest);
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, 4);
            mv.visitVarInsn(ILOAD, 3);
            //else
            mv.visitJumpInsn(IFEQ, lTestCache);

            //if(isVirtual)...
            mv.visitLabel(lVirtual);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, "io/netty/buffer/PooledByteBufAllocator", "createCache",
                    "(II)Lio/netty/buffer/PoolThreadCache;", false);
            mv.visitVarInsn(ASTORE, 4);

            //if(cache == null)..
            mv.visitLabel(lTestCache);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitJumpInsn(IFNONNULL, LgotCache);

            //if the cache was indeed null
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "io/netty/buffer/PooledByteBufAllocator", "threadCache",
                    "Lio/netty/buffer/PooledByteBufAllocator$PoolThreadLocalCache;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "io/netty/buffer/PooledByteBufAllocator$PoolThreadLocalCache",
                    "get", "()Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "io/netty/buffer/PoolThreadCache");
            mv.visitVarInsn(ASTORE, 4);

            //we stored the cache in 4, let's use it now
            mv.visitLabel(LgotCache);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitFieldInsn(GETFIELD, "io/netty/buffer/PoolThreadCache", "directArena",
                    "Lio/netty/buffer/PoolArena;");
            mv.visitVarInsn(ASTORE, 5);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitJumpInsn(IFNULL, LNullDirectArena);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "io/netty/buffer/PoolArena", "allocate",
                    "(Lio/netty/buffer/PoolThreadCache;II)Lio/netty/buffer/PooledByteBuf;", false);
            mv.visitVarInsn(ASTORE, 6);
            mv.visitJumpInsn(GOTO, LReturn);

            mv.visitLabel(LNullDirectArena);
            mv.visitMethodInsn(INVOKESTATIC, "io/netty/util/internal/PlatformDependent", "hasUnsafe",
                    "()Z", false);
            mv.visitJumpInsn(IFEQ, L16);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "io/netty/util/internal/PlatformDependent",
                    "newUnsafeDirectByteBuf",
                    "(Lio/netty/buffer/ByteBufAllocator;II)Lio/netty/buffer/UnpooledUnsafeDirectByteBuf;",
                    false);
            mv.visitJumpInsn(GOTO, L18);

            mv.visitLabel(L16);
            mv.visitTypeInsn(NEW, "io/netty/buffer/UnpooledDirectByteBuf");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, "io/netty/buffer/UnpooledDirectByteBuf", "<init>",
                    "(Lio/netty/buffer/ByteBufAllocator;II)V", false);

            mv.visitLabel(L18);
            mv.visitVarInsn(ASTORE, 6);

            mv.visitLabel(LReturn);
            mv.visitVarInsn(ALOAD, 6);
            mv.visitMethodInsn(INVOKESTATIC, "io/netty/buffer/PooledByteBufAllocator", "toLeakAwareBuffer",
                    "(Lio/netty/buffer/ByteBuf;)Lio/netty/buffer/ByteBuf;", false);

            mv.visitInsn(ARETURN);

            mv.visitLabel(LEnd);

            mv.visitTryCatchBlock(L0, L1, L2, "java/lang/IllegalAccessException");
            mv.visitTryCatchBlock(L0, L1, L2, "java/lang/reflect/InvocationTargetException");
            mv.visitTryCatchBlock(L0, L1, L2, "java/lang/NoSuchMethodException");
            mv.visitTryCatchBlock(L0, L1, L2, "java/lang/ClassNotFoundException");

            mv.visitLocalVariable("isVirtual", "Z", null, LStart, LEnd, 2);
            mv.visitEnd();
            mv.visitMaxs(10, 10);
        }
    }
}
