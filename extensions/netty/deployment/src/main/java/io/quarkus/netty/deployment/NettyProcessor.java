package io.quarkus.netty.deployment;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import io.netty.channel.ChannelHandler;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.EventLoopGroup;
import io.netty.resolver.dns.DnsServerAddressStreamProviders;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedRuntimeSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.ModuleEnableNativeAccessBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import io.quarkus.deployment.builditem.PreInitRunnableBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsafeAccessedFieldBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.netty.BossEventLoopGroup;
import io.quarkus.netty.MainEventLoopGroup;
import io.quarkus.netty.runtime.EmptyByteBufStub;
import io.quarkus.netty.runtime.EventLoopGroupSupplier;
import io.quarkus.netty.runtime.MachineIdGenerator;
import io.quarkus.netty.runtime.NettyRecorder;
import io.quarkus.netty.runtime.NettySharable;

class NettyProcessor {

    private static final String BOSS_EVENT_LOOP_GROUP = "boss";
    private static final String MAIN_EVENT_LOOP_GROUP = "main";

    private static final Logger log = Logger.getLogger(NettyProcessor.class);

    private static final int DEFAULT_NETTY_ALLOCATOR_MAX_ORDER = 3;

    static {
        InternalLoggerFactory.setDefaultFactory(new JBossNettyLoggerFactory());
    }

    /**
     * In dev and test mode, make sure Netty's {@code PlatformDependent} classes do not retain the augmentation class
     * loader through the backtrace of the exceptions they store, see {@link NettyRecorder#initializePlatformDependent()}.
     */
    @BuildStep(onlyIfNot = IsNormal.class)
    void initializePlatformDependent(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar
                .forService("io.quarkus.netty.initialize-platform-dependent")
                .atPhase(Phase.STATIC_INIT)
                .onStart(ctx -> NettyRecorder.initializePlatformDependent());
    }

    @BuildStep
    public NativeImageSystemPropertyBuildItem limitMem() {
        //in native mode we limit the size of the epoll array
        //if the array overflows the selector just moves the overflow to a map
        return new NativeImageSystemPropertyBuildItem("sun.nio.ch.maxUpdateArraySize", "100");
    }

    @BuildStep
    public SystemPropertyBuildItem disableNettyDefaultEndpointVerification() {
        /*
         * Netty 4.2 defaults endpoint verification to "HTTPS", which is read during
         * SslContext static initialization (build time in native mode). Vert.x explicitly
         * manages hostname verification via configureSSLOptions(verifyHost, sslOptions),
         * so the Netty default is not needed and causes SSL failures in native mode when
         * Vert.x's runtime override on the SslContextBuilder doesn't take effect.
         */
        return new SystemPropertyBuildItem("io.netty.handler.ssl.defaultEndpointVerificationAlgorithm", "NONE");
    }

    @BuildStep
    public SystemPropertyBuildItem limitArenaSize(List<MinNettyAllocatorMaxOrderBuildItem> minMaxOrderBuildItems) {
        String maxOrder = calculateMaxOrder(minMaxOrderBuildItems);

        // Set maxOrder for PooledByteBufAllocator (does not affect the default AdaptiveByteBufAllocator)
        return new SystemPropertyBuildItem("io.netty.allocator.maxOrder", maxOrder);
    }

    @BuildStep
    public GeneratedRuntimeSystemPropertyBuildItem setNettyMachineId() {
        // we set the io.netty.machineId system property so to prevent potential
        // slowness when generating/inferring the default machine id in io.netty.channel.DefaultChannelId
        // implementation, which iterates over the NetworkInterfaces to determine the "best" machine id
        return new GeneratedRuntimeSystemPropertyBuildItem("io.netty.machineId", MachineIdGenerator.class);
    }

    @BuildStep
    public SystemPropertyBuildItem disableFinalizers() {
        return new SystemPropertyBuildItem("io.netty.allocator.disableCacheFinalizersForFastThreadLocalThreads", "true");
    }

    @BuildStep
    public SystemPropertyBuildItem disableNettyJfrEventRegistration() {
        // Netty 4.2.20+ registers a JFR FlightRecorder listener that strongly references the classloader.
        // Its prevents the classloader from being garbage collected and causes metaspace OOM in test suites and dev mode.
        // Quarkus does not use Netty JFR buffer events anyway, so we disable these.
        // See io.netty.buffer.JfrEventRegistration
        return new SystemPropertyBuildItem("io.netty.jfr.enabled", "false");
    }

    @BuildStep
    public SystemPropertyBuildItem ignoreExpensiveClean() {
        // On JDK 25+ without --enable-native-access, Netty's CleanerJava25 (shared arenas) has
        // hasExpensiveClean()=true. Without this flag, unpooled direct buffers fall back to the NOOP
        // cleaner (GC-only deallocation), which causes unbounded native memory growth and OOM kills
        // on containers. This flag forces unpooled buffers to use the shared arena path instead —
        // expensive but deterministic.
        // See https://github.com/quarkusio/quarkus/issues/54011
        return new SystemPropertyBuildItem("io.netty.ignoreExpensiveClean", "true");
    }

    @BuildStep
    public PreInitRunnableBuildItem preInitPlatformDependent() {
        // initialize PlatformDependent as a pre-init task as it's quite slow
        return PreInitRunnableBuildItem.initializeClass(PlatformDependent.class.getName(),
                PreInitRunnableBuildItem.DEFAULT_PRIORITY - 50);
    }

    /**
     * <a href="https://openjdk.org/jeps/471">JEP 471</a> locks down access to sun.misc.Unsafe, Netty needs to adapt
     * to this to maintain its efficiency. As this work progresses in upstream Netty to handle this better automatically, we can
     * already apply the following recommendations by the Netty team. See also
     * <a href="https://github.com/quarkusio/quarkus/issues/39907">#39907</a> and
     * <a href="https://netty.io/wiki/java-24-and-sun.misc.unsafe.html">Java 24 and sun.misc.unsafe</a>.
     * </p>
     * Unfortunately, "--sun-misc-unsafe-memory-access=allow" should also be set for Java runtime, but it can't be applied
     * automatically as the JAR Manifest format doesn't allow setting such an option.
     */
    @BuildStep
    NativeImageConfigBuildItem configureUnsafeAccess(
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<ModuleOpenBuildItem> moduleOpenBuildItem) {

        reflectiveMethods.produce(
                new ReflectiveMethodBuildItem("Reflectively accessed through Netty's PlatformDependent0.",
                        "jdk.internal.misc.Unsafe", "allocateUninitializedArray",
                        new String[] { Class.class.getName(), int.class.getName() }));

        reflectiveFields.produce(
                new ReflectiveFieldBuildItem("Reflectively accessed through Netty's PlatformDependent0.",
                        "java.nio.Bits", "UNSAFE_SET_THRESHOLD"));

        // Enables Netty's PlatformDependent0 and PlatformDependent to access java.nio, e.g. java.nio.Bits
        // It's potentially problematic regarding build-time and run-time inited JDK parts.
        moduleOpenBuildItem.produce(
                new ModuleOpenBuildItem("java.base", "io.netty.common", "java.nio", "jdk.internal.misc"));
        // Enable io.netty.channel.nio.NioIoHandler to set sun.nio.ch.SelectorImpl.selectedKeys as accessible
        moduleOpenBuildItem.produce(
                new ModuleOpenBuildItem("java.base", "io.netty.transport", "sun.nio.ch"));

        final NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                .addNativeImageSystemProperty("io.netty.tryReflectionSetAccessible", "true")
                .addNativeImageSystemProperty("io.netty.noUnsafe", "true");
        return builder.build();
    }

    @BuildStep
    NativeImageConfigBuildItem build(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            List<MinNettyAllocatorMaxOrderBuildItem> minMaxOrderBuildItems) {

        reflectiveMethods.produce(
                new ReflectiveMethodBuildItem("Reflectively accessed through PlatformDependent0's static initializer",
                        "jdk.internal.misc.Unsafe", "getUnsafe", new String[0]));

        // in JDK >= 21 the constructor has `long, long` signature
        reflectiveMethods.produce(
                new ReflectiveMethodBuildItem("Reflectively accessed through PlatformDependent0's static initializer",
                        "java.nio.DirectByteBuffer", "<init>", new String[] { long.class.getName(), long.class.getName() }));

        reflectiveFields.produce(
                new ReflectiveFieldBuildItem("Reflectively accessed through PlatformDependent0's static initializer",
                        "java.nio.Bits", "UNALIGNED"));
        reflectiveFields.produce(
                new ReflectiveFieldBuildItem("Reflectively accessed through PlatformDependent0's static initializer",
                        "java.nio.Bits", "MAX_MEMORY"));

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.netty.channel.socket.nio.NioSocketChannel")
                .build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("io.netty.channel.socket.nio.NioServerSocketChannel")
                        .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.netty.channel.socket.nio.NioDatagramChannel")
                .build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("java.util.LinkedHashMap").build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("sun.nio.ch.SelectorImpl").methods().fields().build());

        String maxOrder = calculateMaxOrder(minMaxOrderBuildItems);

        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                // Set maxOrder for PooledByteBufAllocator (not used by the default AdaptiveByteBufAllocator)
                .addNativeImageSystemProperty("io.netty.allocator.maxOrder", maxOrder)
                // Spotted with Netty 4.1.135.Final
                .addRuntimeInitializedClass("io.netty.internal.tcnative.SSL")
                // Runtime initialize to respect io.netty.handler.ssl.conscrypt.useBufferAllocator
                .addRuntimeInitializedClass("io.netty.handler.ssl.ConscryptAlpnSslEngine")
                // Runtime initialize due to the use of tcnative in the static initializers?
                .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                // Runtime initialize to respect run-time provided values of the following properties:
                // - io.netty.handler.ssl.openssl.bioNonApplicationBufferSize
                // - io.netty.handler.ssl.openssl.useTasks
                // - jdk.tls.client.enableSessionTicketExtension
                // - io.netty.handler.ssl.openssl.sessionCacheServer
                // - io.netty.handler.ssl.openssl.sessionCacheClient
                // - jdk.tls.ephemeralDHKeySize
                .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslContext")
                // .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslClientContext")
                // Runtime initialize to respect run-time provided values of the following properties:
                // - keystore.type
                // - ssl.KeyManagerFactory.algorithm
                // - ssl.TrustManagerFactory.algorithm
                .addRuntimeInitializedClass("io.netty.handler.ssl.JdkSslServerContext")
                // .addRuntimeInitializedClass("io.netty.handler.ssl.JdkSslClientContext")
                // Runtime initialize to prevent embedding SecureRandom instances in the native image
                .addRuntimeInitializedClass("io.netty.handler.ssl.SslContext")
                .addRuntimeInitializedClass("io.netty.handler.ssl.util.ThreadLocalInsecureRandom")
                // The default channel id uses the process id, it should not be cached in the native image. This way we
                // also respect the run-time provided value of the io.netty.processId property, io.netty.machineId
                // property is being hardcoded in setNettyMachineId method
                .addRuntimeInitializedClass("io.netty.channel.DefaultChannelId")
                // Disable leak detection by default, it can still be enabled via
                // io.netty.util.ResourceLeakDetector.setLevel method
                .addNativeImageSystemProperty("io.netty.leakDetection.level", "DISABLED");

        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.handler.codec.http.HttpObjectEncoder")) {
            builder
                    // Runtime initialize due to transitive use of the io.netty.util.internal.PlatformDependent class
                    // when initializing CRLF_BUF and ZERO_CRLF_CRLF_BUF
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.HttpObjectEncoder")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.websocketx.extensions.compression.DeflateDecoder")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.HttpContentCompressor");
            // Zstd is an optional dependency, runtime initialize to avoid IllegalStateException when zstd is not
            // available. This will result in a runtime ClassNotFoundException if the user tries to use zstd.
            if (!QuarkusClassLoader.isClassPresentAtRuntime("com.github.luben.zstd.Zstd")) {
                builder.addRuntimeInitializedClass("io.netty.handler.codec.compression.ZstdOptions")
                        .addRuntimeInitializedClass("io.netty.handler.codec.compression.ZstdConstants");
            }
            // Brotli is an optional dependency, we should only runtime initialize BrotliOptions when it is available
            // (e.g. com.aayushatharva.brotli4j.Brotli4jLoader is present): BrotliOptions references Brotli4j and its
            // static initializer fails without it. It is cut from the native image otherwise.
            if (QuarkusClassLoader.isClassPresentAtRuntime("com.aayushatharva.brotli4j.Brotli4jLoader")) {
                builder.addRuntimeInitializedClass("io.netty.handler.codec.compression.BrotliOptions");
            }
        } else {
            log.debug("Not registering Netty HTTP classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.handler.codec.http2.Http2CodecUtil")) {
            builder
                    // Runtime initialize due to the transitive use of the io.netty.util.internal.PlatformDependent
                    // class in the static initializers
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.Http2CodecUtil")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.Http2ConnectionHandler")
                    // Runtime initialize due to dependency on io.netty.handler.codec.http2.Http2CodecUtil
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.Http2ClientUpgradeCodec");
        } else {
            log.debug("Not registering Netty HTTP2 classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.channel.unix.UnixChannel")) {
            // Runtime initialize to avoid embedding quite a few Strings in the image heap
            builder.addRuntimeInitializedClass("io.netty.channel.unix.Errors")
                    // Runtime initialize due to the use of AtomicIntegerFieldUpdater?
                    .addRuntimeInitializedClass("io.netty.channel.unix.FileDescriptor")
                    // Runtime initialize due to the use of Buffer.addressSize() in the static initializers
                    .addRuntimeInitializedClass("io.netty.channel.unix.IovArray")
                    // Runtime initialize due to the use of native methods in the static initializers?
                    .addRuntimeInitializedClass("io.netty.channel.unix.Limits");
        } else {
            log.debug("Not registering Netty native unix classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.channel.epoll.EpollMode")) {
            // Runtime initialize due to machine dependent native methods being called in static initializer and to
            // respect the run-time provided value of io.netty.transport.noNative
            builder.addRuntimeInitializedClass("io.netty.channel.epoll.Epoll")
                    // Runtime initialize due to machine dependent native methods being called in static initializer
                    .addRuntimeInitializedClass("io.netty.channel.epoll.EpollEventArray")
                    // Runtime initialize due to dependency on Epoll and to respect the run-time provided value of
                    // io.netty.channel.epoll.epollWaitThreshold
                    .addRuntimeInitializedClass("io.netty.channel.epoll.EpollEventLoop")
                    // Runtime initialize due to InetAddress fields, dependencies on native methods and to transitively
                    // respect a number of properties, e.g. java.nio.channels.spi.SelectorProvider
                    .addRuntimeInitializedClass("io.netty.channel.epoll.Native");
        } else {
            log.debug("Not registering Netty native epoll classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.channel.kqueue.AcceptFilter")) {
            // Runtime initialize due to machine dependent native methods being called in static initializer and to
            // respect the run-time provided value of io.netty.transport.noNative
            builder.addRuntimeInitializedClass("io.netty.channel.kqueue.KQueue")
                    // Runtime initialize due to machine dependent native methods being called in static initializers
                    .addRuntimeInitializedClass("io.netty.channel.kqueue.KQueueEventArray")
                    .addRuntimeInitializedClass("io.netty.channel.kqueue.Native")
                    // Runtime initialize due to the use of AtomicIntegerFieldUpdater in the static initializer.
                    // Replaces io.netty.channel.kqueue.KQueueEventLoop, which was removed in Netty 4.2
                    .addRuntimeInitializedClass("io.netty.channel.kqueue.KQueueIoHandler");
        } else {
            log.debug("Not registering Netty native kqueue classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.channel.uring.IoUring")) {
            // Runtime initialize due to kernel version checking, native library loading, and kernel feature probing
            // in static initializer, and to respect the run-time provided value of io.netty.transport.noNative
            builder.addRuntimeInitializedClass("io.netty.channel.uring.IoUring")
                    // Runtime initialize due to native library loading in static initializer
                    .addRuntimeInitializedClass("io.netty.channel.uring.Native")
                    // Runtime initialize due to system properties read in static initializer
                    .addRuntimeInitializedClass("io.netty.channel.uring.IoUringDatagramChannel");
        } else {
            log.debug("Not registering Netty native io_uring classes as they were not found");
        }

        // * [IMPORTANT] Netty QUIC/Quiche classes: runtime-init is NOT registered here.
        // *
        // * => When http3 is absent, Vert.x substitutions cut all reachability to these classes.
        // * => When http3 is present, Http3Processor registers them for runtime initialization.

        // tcnative is handled via RuntimeInitializedPackageBuildItem in #runtimeInitQuicAndTcnative

        // Runtime initialize due to platform dependent initialization and to respect the run-time provided value of the
        // properties:
        // - io.netty.maxDirectMemory
        // - io.netty.uninitializedArrayAllocationThreshold
        // - io.netty.noPreferDirect
        // - io.netty.osClassifiers
        // - io.netty.tmpdir
        // - java.io.tmpdir
        // - io.netty.bitMode
        // - sun.arch.data.model
        // - com.ibm.vm.bitmode
        builder.addRuntimeInitializedClass("io.netty.util.internal.PlatformDependent")
                // Similarly for properties:
                // - io.netty.noUnsafe
                // - sun.misc.unsafe.memory.access
                // - io.netty.tryUnsafe
                // - org.jboss.netty.tryUnsafe
                // - io.netty.tryReflectionSetAccessible
                .addRuntimeInitializedClass("io.netty.util.internal.PlatformDependent0")
                // Runtime initialize classes to allow netty to use the field offset for testing if unsafe is available or not
                // See https://github.com/quarkusio/quarkus/issues/47903#issuecomment-2890924970
                .addRuntimeInitializedClass("io.netty.util.AbstractReferenceCounted")
                .addRuntimeInitializedClass("io.netty.buffer.AbstractReferenceCountedByteBuf")
                // Runtime initialize to respect the run-time number of processors, which it caches on first use
                // (respecting the run-time provided value of the io.netty.availableProcessors property)
                .addRuntimeInitializedClass("io.netty.util.NettyRuntime")
                // Runtime initialize to respect the run-time number of processors and the run-time provided value of
                // the io.netty.eventLoopThreads property, used by event loop groups created with nThreads == 0
                .addRuntimeInitializedClass("io.netty.channel.MultithreadEventLoopGroup");

        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.buffer.UnpooledByteBufAllocator")) {
            // Runtime initialize due to the use of the io.netty.util.internal.PlatformDependent class
            builder.addRuntimeInitializedClass("io.netty.buffer.UnpooledByteBufAllocator")
                    .addRuntimeInitializedClass("io.netty.buffer.Unpooled")
                    // Runtime initialize due to dependency on io.netty.buffer.Unpooled
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.HttpObjectAggregator")
                    .addRuntimeInitializedClass("io.netty.handler.codec.ReplayingDecoderByteBuf")
                    // Runtime initialize to avoid embedding quite a few Strings in the image heap
                    .addRuntimeInitializedClass("io.netty.buffer.ByteBufUtil$HexUtil")
                    // Runtime initialize due to the use of the io.netty.util.internal.PlatformDependent class in the
                    // static initializers and to respect the run-time provided value of the following properties:
                    // - io.netty.allocator.directMemoryCacheAlignment
                    // - io.netty.allocator.pageSize
                    // - io.netty.allocator.maxOrder
                    // - io.netty.allocator.numHeapArenas
                    // - io.netty.allocator.numDirectArenas
                    // - io.netty.allocator.smallCacheSize
                    // - io.netty.allocator.normalCacheSize
                    // - io.netty.allocator.maxCachedBufferCapacity
                    // - io.netty.allocator.cacheTrimInterval
                    // - io.netty.allocation.cacheTrimIntervalMillis
                    // - io.netty.allocator.cacheTrimIntervalMillis
                    // - io.netty.allocator.useCacheForAllThreads
                    // - io.netty.allocator.maxCachedByteBuffersPerChunk
                    .addRuntimeInitializedClass("io.netty.buffer.PooledByteBufAllocator")
                    // Runtime initialize the default allocator of Netty 4.2: its static initializers size it from the
                    // run-time max heap and number of processors and read the run-time provided value of the
                    // following properties:
                    // - io.netty.allocator.useCachedMagazinesForNonEventLoopThreads
                    // - io.netty.allocator.lowMemory
                    // - io.netty.allocator.disableThreadLocalMagazinesOnLowMemory
                    // - io.netty.allocator.chunkReuseQueueCapacity
                    // - io.netty.allocator.chunkPurgePollsThreadLocal
                    // - io.netty.allocator.chunkPurgePollsShared
                    // - io.netty.allocator.chunkPurgeThreshold
                    // - io.netty.allocator.threadLocalChunkCacheMaxBytes
                    // - io.netty.allocator.threadLocalChunkCacheMinBytes
                    // - io.netty.allocator.magazineBufferQueueCapacity
                    .addRuntimeInitializedClass("io.netty.buffer.AdaptiveByteBufAllocator")
                    .addRuntimeInitializedClass("io.netty.buffer.AdaptivePoolingAllocator")
                    // Runtime initialize due to its static initializer depending on AdaptivePoolingAllocator
                    .addRuntimeInitializedClass("io.netty.buffer.AdaptivePoolingAllocator$SharedSizeClassedChunkCache")
                    // Runtime initialize due to the use of ByteBufUtil.DEFAULT_ALLOCATOR in the static initializers
                    .addRuntimeInitializedClass("io.netty.buffer.ByteBufAllocator")
                    // Runtime initialize due to the use of the io.netty.util.internal.PlatformDependent class in the
                    // static initializers and to respect the run-time provided value of the following properties:
                    // - io.netty.allocator.type
                    // - io.netty.threadLocalDirectBufferSize
                    // - io.netty.maxThreadLocalCharBufferSize
                    .addRuntimeInitializedClass("io.netty.buffer.ByteBufUtil");

            if (QuarkusClassLoader
                    .isClassPresentAtRuntime("org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload")) {
                // Runtime initialize due to dependency on io.netty.buffer.Unpooled
                builder.addRuntimeInitializedClass(
                        "org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload");
            }
        }

        return builder //TODO: make configurable
                .build();
    }

    @BuildStep
    void runtimeInitQuicAndTcnative(BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitializedPackages) {
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.internal.tcnative.SSL")) {
            runtimeInitializedPackages.produce(new RuntimeInitializedPackageBuildItem("io.netty.internal.tcnative"));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BuildStep
    void registerEventLoopBeans(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            Optional<EventLoopSupplierBuildItem> loopSupplierBuildItem,
            ServiceRegistrar serviceRegistrar,
            BuildProducer<EventLoopGroupBuildItem> eventLoopGroups) {
        Supplier<EventLoopGroup> boss;
        Supplier<EventLoopGroup> main;
        // IMPLEMENTATION NOTE:
        // We use Singleton scope for both beans. ApplicationScoped causes problems with EventLoopGroup.next()
        // which overrides the EventExecutorGroup.next() method but since Netty 4 is compiled with JDK6 the corresponding bridge method
        // is not generated and the invocation upon the client proxy results in an AbstractMethodError
        if (loopSupplierBuildItem.isPresent()) {
            boss = (Supplier) loopSupplierBuildItem.get().getBossSupplier();
            main = (Supplier) loopSupplierBuildItem.get().getMainSupplier();
            syntheticBeans.produce(eventLoopGroupBean(BossEventLoopGroup.class).supplier(boss).done());
            syntheticBeans.produce(eventLoopGroupBean(MainEventLoopGroup.class).supplier(main).done());
        } else {
            serviceRegistrar.forService(EventLoopGroup.class, BOSS_EVENT_LOOP_GROUP)
                    .atPhase(Phase.INFRASTRUCTURE)
                    .onStart(ctx -> NettyRecorder.createEventLoop(ctx, 1));
            serviceRegistrar.forService(EventLoopGroup.class, MAIN_EVENT_LOOP_GROUP)
                    .atPhase(Phase.INFRASTRUCTURE)
                    .onStart(ctx -> NettyRecorder.createEventLoop(ctx, 0));
            boss = new EventLoopGroupSupplier(serviceRegistrar.getRecorderProxy(EventLoopGroup.class, BOSS_EVENT_LOOP_GROUP));
            main = new EventLoopGroupSupplier(serviceRegistrar.getRecorderProxy(EventLoopGroup.class, MAIN_EVENT_LOOP_GROUP));
            syntheticBeans.produce(eventLoopGroupBean(BossEventLoopGroup.class)
                    .serviceValue(EventLoopGroup.class, BOSS_EVENT_LOOP_GROUP).done());
            syntheticBeans.produce(eventLoopGroupBean(MainEventLoopGroup.class)
                    .serviceValue(EventLoopGroup.class, MAIN_EVENT_LOOP_GROUP).done());
        }

        eventLoopGroups.produce(new EventLoopGroupBuildItem(boss, main));
    }

    private static SyntheticBeanBuildItem.ExtendedBeanConfigurator eventLoopGroupBean(Class<? extends Annotation> qualifier) {
        return SyntheticBeanBuildItem.configure(EventLoopGroup.class)
                .scope(Singleton.class)
                .addQualifier(qualifier)
                .unremovable()
                .setRuntimeInit();
    }

    @BuildStep
    AdditionalBeanBuildItem registerQualifiers() {
        // We need to register the qualifiers manually because they're not part of the index
        // Previously they were indexed because we indexed the "uber-producer-class" generated for RuntimeBeanBuildItems
        return AdditionalBeanBuildItem.builder().addBeanClasses(BossEventLoopGroup.class, MainEventLoopGroup.class).build();
    }

    @BuildStep
    public RuntimeInitializedClassBuildItem reinitScheduledFutureTask() {
        return new RuntimeInitializedClassBuildItem(
                "io.quarkus.netty.runtime.graal.Holder_io_netty_util_concurrent_ScheduledFutureTask");
    }

    @BuildStep
    public List<UnsafeAccessedFieldBuildItem> unsafeAccessedFields() {
        return Arrays.asList(
                new UnsafeAccessedFieldBuildItem("sun.nio.ch.SelectorImpl", "selectedKeys"),
                new UnsafeAccessedFieldBuildItem("sun.nio.ch.SelectorImpl", "publicSelectedKeys"),
                new UnsafeAccessedFieldBuildItem("io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess",
                        "REF_ELEMENT_SHIFT"));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitBcryptUtil() {
        // this holds a direct allocated byte buffer that needs to be initialised at run time
        return new RuntimeInitializedClassBuildItem(EmptyByteBufStub.class.getName());
    }

    //if debug logging is enabled netty logs lots of exceptions
    //see https://github.com/quarkusio/quarkus/issues/5213
    @BuildStep
    LogCleanupFilterBuildItem cleanupUnsafeLog() {
        return new LogCleanupFilterBuildItem(PlatformDependent.class.getName() + "0", Level.TRACE, "direct buffer constructor",
                "jdk.internal.misc.Unsafe", "sun.misc.Unsafe");
    }

    /**
     * On mac, if you do not have the `MacOSDnsServerAddressStreamProvider` class, Netty prints a warning saying it
     * falls back to the default system DNS provider. This is not a problem and generates tons of questions.
     *
     * @return the log cleanup item removing the message
     */
    @BuildStep
    LogCleanupFilterBuildItem cleanupMacDNSInLog() {
        return new LogCleanupFilterBuildItem(DnsServerAddressStreamProviders.class.getName(), Level.WARN,
                "Can not find io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider in the classpath");
    }

    /**
     * `Version.identify()` in netty-common uses the resource to determine the version of netty.
     */
    @BuildStep
    NativeImageResourceBuildItem nettyVersions() {
        return new NativeImageResourceBuildItem("META-INF/io.netty.versions.properties");
    }

    private String calculateMaxOrder(List<MinNettyAllocatorMaxOrderBuildItem> minMaxOrderBuildItems) {
        int result = DEFAULT_NETTY_ALLOCATOR_MAX_ORDER;
        for (MinNettyAllocatorMaxOrderBuildItem minMaxOrderBuildItem : minMaxOrderBuildItems) {
            if (minMaxOrderBuildItem.getMaxOrder() > result) {
                result = minMaxOrderBuildItem.getMaxOrder();
            }
        }
        return Integer.toString(result);
    }

    /**
     * Rewrites {@code PlatformDependent0} to eliminate expensive {@code MethodHandle} lookups
     * from its static initializer and replace {@code MethodHandle.invokeExact} dispatch with
     * direct method calls. Since we target Java 17+, the following APIs are always available:
     * <ul>
     * <li>{@code Thread.isVirtual()} (Java 21+, but Quarkus minimum is 21)</li>
     * <li>{@code ByteBuffer.alignedSlice(int)} (Java 9+)</li>
     * <li>{@code ByteBuffer.slice(int, int)} (Java 13+)</li>
     * <li>{@code ByteBuffer.put(int, ByteBuffer, int, int)} (Java 16+)</li>
     * <li>{@code ByteBuffer.put(int, byte[], int, int)} (Java 13+)</li>
     * <li>{@code SplittableRandom.nextBytes(byte[])} (Java 17+)</li>
     * </ul>
     * <p>
     * For each field, we apply three atomic changes:
     * <ol>
     * <li>Patch the static initializer (via ASM tree API) to skip the
     * {@code AccessController.doPrivileged} + {@code MethodHandles.publicLookup().findVirtual()}
     * lookup, forcing the field to {@code null}.</li>
     * <li>Replace the {@code has*Method()} guard to return {@code true} unconditionally.</li>
     * <li>Replace the accessor method to call the target method directly instead of going
     * through {@code MethodHandle.invokeExact}.</li>
     * </ol>
     */
    @BuildStep
    void transformPlatformDependent0(CompiledJavaVersionBuildItem compiledJavaVersion,
            BuildProducer<BytecodeTransformerBuildItem> producer) {
        String className = "io.netty.util.internal.PlatformDependent0";

        boolean isJava25OrHigher = compiledJavaVersion.getJavaVersion()
                .isJava25OrHigher() == CompiledJavaVersionBuildItem.JavaVersion.Status.TRUE;

        Set<String> fieldsToSkip;
        Set<String> knownUnhandledFields;
        if (isJava25OrHigher) {
            fieldsToSkip = Set.of(
                    "ALIGN_SLICE", "OFFSET_SLICE", "ABSOLUTE_PUT_BUFFER",
                    "ABSOLUTE_PUT_ARRAY", "SPLITTABLE_RANDOM_NEXT_BYTES",
                    "MEMORY_SEGMENT_ADDRESS_OF_BUFFER");
            knownUnhandledFields = Set.of(
                    "DIRECT_BUFFER_CONSTRUCTOR", "ALLOCATE_ARRAY_METHOD");
        } else {
            fieldsToSkip = Set.of(
                    "ALIGN_SLICE", "OFFSET_SLICE", "ABSOLUTE_PUT_BUFFER",
                    "ABSOLUTE_PUT_ARRAY", "SPLITTABLE_RANDOM_NEXT_BYTES");
            knownUnhandledFields = Set.of(
                    "DIRECT_BUFFER_CONSTRUCTOR", "ALLOCATE_ARRAY_METHOD",
                    "MEMORY_SEGMENT_ADDRESS_OF_BUFFER");
        }

        producer.produce(new BytecodeTransformerBuildItem.Builder().setClassToTransform(className)
                .setCacheable(true)
                .setClassReaderOptions(ClassReader.EXPAND_FRAMES)
                .setVisitorFunction(
                        new BiFunction<>() {
                            @Override
                            public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                                ClassTransformer transformer = new ClassTransformer(className);

                                // --- getIsVirtualThreadMethodHandle -> return null ---
                                {
                                    MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(className,
                                            "getIsVirtualThreadMethodHandle",
                                            "java.lang.invoke.MethodHandle");
                                    transformer.removeMethod(methodDescriptor);
                                    MethodCreator method = transformer.addMethod(methodDescriptor)
                                            .setModifiers(Modifier.STATIC | Modifier.PRIVATE);
                                    method.returnValue(method.loadNull());
                                }

                                // --- isVirtualThread(Thread) -> thread != null && thread.isVirtual() ---
                                {
                                    MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(className, "isVirtualThread",
                                            boolean.class, Thread.class);
                                    transformer.removeMethod(methodDescriptor);

                                    MethodCreator isVirtualThreadMethod = transformer.addMethod(methodDescriptor)
                                            .setModifiers(Modifier.STATIC);

                                    ResultHandle threadParam = isVirtualThreadMethod.getMethodParam(0);

                                    BranchResult nullCheck = isVirtualThreadMethod.ifNull(threadParam);

                                    nullCheck.trueBranch().returnValue(nullCheck.trueBranch().load(false));

                                    MethodDescriptor isVirtualMethod = MethodDescriptor.ofMethod(
                                            Thread.class,
                                            "isVirtual",
                                            boolean.class);
                                    ResultHandle isVirtualResult = nullCheck.falseBranch().invokeVirtualMethod(
                                            isVirtualMethod,
                                            threadParam);

                                    nullCheck.falseBranch().returnValue(isVirtualResult);
                                }

                                // --- has* methods -> return true ---
                                replaceWithReturnTrue(transformer, className, "hasAlignSliceMethod");
                                replaceWithReturnTrue(transformer, className, "hasOffsetSliceMethod");
                                replaceWithReturnTrue(transformer, className, "hasAbsolutePutBufferMethod");
                                replaceWithReturnTrue(transformer, className, "hasAbsolutePutArrayMethod");

                                // --- alignSlice(ByteBuffer, int) -> buffer.alignedSlice(alignment) ---
                                {
                                    MethodDescriptor md = MethodDescriptor.ofMethod(className, "alignSlice",
                                            ByteBuffer.class, ByteBuffer.class, int.class);
                                    transformer.removeMethod(md);
                                    MethodCreator m = transformer.addMethod(md).setModifiers(Modifier.STATIC);
                                    ResultHandle result = m.invokeVirtualMethod(
                                            MethodDescriptor.ofMethod(ByteBuffer.class, "alignedSlice",
                                                    ByteBuffer.class, int.class),
                                            m.getMethodParam(0), m.getMethodParam(1));
                                    m.returnValue(result);
                                }

                                // --- offsetSlice(ByteBuffer, int, int) -> buffer.slice(index, length) ---
                                {
                                    MethodDescriptor md = MethodDescriptor.ofMethod(className, "offsetSlice",
                                            ByteBuffer.class, ByteBuffer.class, int.class, int.class);
                                    transformer.removeMethod(md);
                                    MethodCreator m = transformer.addMethod(md).setModifiers(Modifier.STATIC);
                                    ResultHandle result = m.invokeVirtualMethod(
                                            MethodDescriptor.ofMethod(ByteBuffer.class, "slice",
                                                    ByteBuffer.class, int.class, int.class),
                                            m.getMethodParam(0), m.getMethodParam(1), m.getMethodParam(2));
                                    m.returnValue(result);
                                }

                                // --- absolutePut(ByteBuffer, int, ByteBuffer, int, int) -> dst.put(...) ---
                                {
                                    MethodDescriptor md = MethodDescriptor.ofMethod(className, "absolutePut",
                                            ByteBuffer.class, ByteBuffer.class, int.class,
                                            ByteBuffer.class, int.class, int.class);
                                    transformer.removeMethod(md);
                                    MethodCreator m = transformer.addMethod(md).setModifiers(Modifier.STATIC);
                                    ResultHandle result = m.invokeVirtualMethod(
                                            MethodDescriptor.ofMethod(ByteBuffer.class, "put",
                                                    ByteBuffer.class, int.class, ByteBuffer.class, int.class, int.class),
                                            m.getMethodParam(0), m.getMethodParam(1), m.getMethodParam(2),
                                            m.getMethodParam(3), m.getMethodParam(4));
                                    m.returnValue(result);
                                }

                                // --- absolutePut(ByteBuffer, int, byte[], int, int) -> dst.put(...) ---
                                {
                                    MethodDescriptor md = MethodDescriptor.ofMethod(className, "absolutePut",
                                            ByteBuffer.class, ByteBuffer.class, int.class,
                                            byte[].class, int.class, int.class);
                                    transformer.removeMethod(md);
                                    MethodCreator m = transformer.addMethod(md).setModifiers(Modifier.STATIC);
                                    ResultHandle result = m.invokeVirtualMethod(
                                            MethodDescriptor.ofMethod(ByteBuffer.class, "put",
                                                    ByteBuffer.class, int.class, byte[].class, int.class, int.class),
                                            m.getMethodParam(0), m.getMethodParam(1), m.getMethodParam(2),
                                            m.getMethodParam(3), m.getMethodParam(4));
                                    m.returnValue(result);
                                }

                                // --- splittableRandomNextBytes(SplittableRandom, byte[]) -> rng.nextBytes(data) ---
                                {
                                    MethodDescriptor md = MethodDescriptor.ofMethod(className,
                                            "splittableRandomNextBytes",
                                            void.class, SplittableRandom.class, byte[].class);
                                    transformer.removeMethod(md);
                                    MethodCreator m = transformer.addMethod(md).setModifiers(Modifier.STATIC);
                                    m.invokeVirtualMethod(
                                            MethodDescriptor.ofMethod(SplittableRandom.class, "nextBytes",
                                                    void.class, byte[].class),
                                            m.getMethodParam(0), m.getMethodParam(1));
                                    m.returnVoid();
                                }

                                // --- readBitsMaxDirectMemory() -> VM.maxDirectMemory(), or Bits.MAX_MEMORY via reflection ---
                                generateReadBitsMaxDirectMemory(transformer, className);

                                // --- Java 25+ specific transforms ---
                                if (isJava25OrHigher) {
                                    replaceWithReturnTrue(transformer, className, "hasMemorySegmentAddressOfBuffer");
                                    generateDirectBufferAddress(transformer, className);
                                }

                                ClassVisitor downstream = classVisitor;
                                if (isJava25OrHigher) {
                                    downstream = new ClassVisitor(Gizmo.ASM_API_VERSION, downstream) {
                                        @Override
                                        public void visit(int version, int access, String name, String signature,
                                                String superName, String[] interfaces) {
                                            super.visit(Math.max(version, 69), access, name, signature,
                                                    superName, interfaces);
                                        }
                                    };
                                }

                                ClassVisitor gizmoVisitor = transformer.applyTo(downstream);
                                return createClinitPatcher(gizmoVisitor, fieldsToSkip,
                                        knownUnhandledFields);
                            }
                        })
                .build());
    }

    /**
     * Rewrites {@code DefaultChannelId#processHandlePid(ClassLoader)} to avoid reflection as we target Java 21+.
     * <p>
     * The upstream Netty 4.2.x code uses reflection:
     *
     * <pre>{@code
     * static int processHandlePid(ClassLoader loader) {
     *     int nilValue = -1;
     *     if (PlatformDependent.javaVersion() >= 9) {
     *         Long pid;
     *         try {
     *             Class<?> processHandleImplType = Class.forName("java.lang.ProcessHandle", true, loader);
     *             Method processHandleCurrent = processHandleImplType.getMethod("current");
     *             Object processHandleInstance = processHandleCurrent.invoke(null);
     *             Method processHandlePid = processHandleImplType.getMethod("pid");
     *             pid = (Long) processHandlePid.invoke(processHandleInstance);
     *         } catch (Exception e) {
     *             logger.debug("Could not invoke ProcessHandle.current().pid();", e);
     *             return nilValue;
     *         }
     *         if (pid > Integer.MAX_VALUE || pid < Integer.MIN_VALUE) {
     *             throw new IllegalStateException("Current process ID exceeds int range: " + pid);
     *         }
     *         return pid.intValue();
     *     }
     *     return nilValue;
     * }
     * }</pre>
     *
     * We replace it with direct {@code ProcessHandle} API calls:
     *
     * <pre>{@code
     * static int processHandlePid(ClassLoader classLoader) {
     *     int resultVar;
     *     try {
     *         ProcessHandle processHandle = ProcessHandle.current();
     *         long pid = processHandle.pid();
     *         if (pid > Integer.MAX_VALUE) {
     *             resultVar = -1;
     *         } else {
     *             resultVar = (int) pid;
     *         }
     *     } catch (Exception e) {
     *         logger.debug("Could not invoke ProcessHandle.current().pid();", e);
     *         resultVar = -1;
     *     }
     *     return resultVar;
     * }
     * }</pre>
     */
    @BuildStep
    void transformDefaultChannelId(BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformers) {
        // we know we are using Java 17+ so we can always apply this transformation

        String className = DefaultChannelId.class.getName();

        bytecodeTransformers.produce(
                new BytecodeTransformerBuildItem.Builder()
                        .setClassToTransform(className)
                        .setCacheable(true)
                        .setVisitorFunction((s, classVisitor) -> {
                            ClassVisitor updateBytecodeVersion = new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                                @Override
                                public void visit(int version, int access, String name, String signature, String superName,
                                        String[] interfaces) {
                                    // bump bytecode version to at least 52 as we need it to be able to call static methods on interfaces with Gizmo
                                    super.visit(Math.max(version, 52), access, name, signature, superName, interfaces);
                                }
                            };

                            ClassTransformer transformer = new ClassTransformer(className);

                            MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(
                                    className,
                                    "processHandlePid",
                                    int.class,
                                    ClassLoader.class);

                            transformer.removeMethod(methodDescriptor);

                            MethodCreator method = transformer.addMethod(methodDescriptor)
                                    .setModifiers(Modifier.STATIC);

                            AssignableResultHandle resultVar = method.createVariable(int.class);

                            TryBlock tryBlock = method.tryBlock();

                            ResultHandle processHandle = tryBlock.invokeStaticInterfaceMethod(
                                    MethodDescriptor.ofMethod(
                                            ProcessHandle.class,
                                            "current",
                                            ProcessHandle.class));

                            ResultHandle pid = tryBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(
                                            ProcessHandle.class,
                                            "pid",
                                            long.class),
                                    processHandle);

                            ResultHandle maxInt = tryBlock.load((long) Integer.MAX_VALUE);
                            ResultHandle cmp = tryBlock.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(
                                            Long.class,
                                            "compare",
                                            int.class,
                                            long.class,
                                            long.class),
                                    pid,
                                    maxInt);

                            BranchResult branchResult = tryBlock.ifGreaterThanZero(cmp);

                            // pid > Integer.MAX_VALUE, assign -1
                            BytecodeCreator outOfRangeBranch = branchResult.trueBranch();
                            outOfRangeBranch.assign(resultVar, outOfRangeBranch.load(-1));

                            // pid <= Integer.MAX_VALUE, convert long to int
                            BytecodeCreator inRangeBranch = branchResult.falseBranch();
                            inRangeBranch.assign(resultVar, inRangeBranch.convertPrimitive(pid, int.class));

                            CatchBlockCreator catchBlock = tryBlock.addCatch(Exception.class);
                            // logger.debug("Could not invoke ProcessHandle.current().pid();", e);
                            catchBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(
                                            io.netty.util.internal.logging.InternalLogger.class,
                                            "debug",
                                            void.class,
                                            String.class,
                                            Throwable.class),
                                    catchBlock.readStaticField(
                                            FieldDescriptor.of(className, "logger",
                                                    io.netty.util.internal.logging.InternalLogger.class)),
                                    catchBlock.load("Could not invoke ProcessHandle.current().pid();"),
                                    catchBlock.getCaughtException());
                            catchBlock.assign(resultVar, catchBlock.load(-1));

                            method.returnValue(resultVar);

                            return transformer.applyTo(updateBytecodeVersion);
                        })
                        .build());
    }

    @BuildStep
    void enableNativeAccess(BuildProducer<ModuleEnableNativeAccessBuildItem> nativeAccess) {
        // Netty 4.2 on JDK 24+ uses CleanerJava24Linker (FFM malloc/free) for direct buffer
        // allocation when native access is granted to io.netty.common. Without this, JDK 25+
        // falls back to CleanerJava25 (shared arenas with expensive thread-local handshakes)
        // or NOOP (GC-only deallocation for unpooled buffers, causing container OOM).
        // See https://github.com/quarkusio/quarkus/issues/54011
        nativeAccess.produce(new ModuleEnableNativeAccessBuildItem("io.netty.common"));
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.channel.epoll.EpollMode")) {
            nativeAccess.produce(new ModuleEnableNativeAccessBuildItem("io.netty.transport.classes.epoll"));
        }
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.channel.kqueue.AcceptFilter")) {
            nativeAccess.produce(new ModuleEnableNativeAccessBuildItem("io.netty.transport.classes.kqueue"));
        }
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.channel.uring.IoUring")) {
            nativeAccess.produce(new ModuleEnableNativeAccessBuildItem("io.netty.transport.classes.io_uring"));
        }
    }

    @BuildStep
    void indexTransports(BuildProducer<IndexDependencyBuildItem> producer) {
        producer.produce(new IndexDependencyBuildItem("io.netty", "netty-transport"));
    }

    /**
     * Optimizes {@code ChannelHandlerAdapter#isSharable()} to avoid the per-call annotation lookup via
     * {@code ThreadLocal} + {@code WeakHashMap} cache that Netty uses.
     * <p>
     * The upstream Netty 4.2.x code:
     *
     * <pre>{@code
     * public boolean isSharable() {
     *     Class<?> clazz = getClass();
     *     Map<Class<?>, Boolean> cache = InternalThreadLocalMap.get().handlerSharableCache();
     *     Boolean sharable = cache.get(clazz);
     *     if (sharable == null) {
     *         sharable = clazz.isAnnotationPresent(Sharable.class);
     *         cache.put(clazz, sharable);
     *     }
     *     return sharable;
     * }
     * }</pre>
     *
     * We replace it with a compile-time marker interface approach: all classes annotated with {@code @Sharable}
     * get the {@code NettySharable} marker interface added, and {@code isSharable()} becomes:
     *
     * <pre>{@code
     * public boolean isSharable() {
     *     if (this instanceof NettySharable) {
     *         return true;
     *     }
     *     return this.isSharable0(); // original method, fallback for non-indexed classes
     * }
     * }</pre>
     */
    @BuildStep
    void transformIsSharable(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<BytecodeTransformerBuildItem> producer) {
        IndexView index = indexBuildItem.getIndex();

        // add the NettySharable marker to each class that is annotated with @Sharable
        index.getAnnotations(ChannelHandler.Sharable.class).forEach(ai -> {
            if (ai.target().kind() != AnnotationTarget.Kind.CLASS) {
                return;
            }

            String className = ai.target().asClass().name().toString();
            producer.produce(new BytecodeTransformerBuildItem.Builder().setClassToTransform(className)
                    .setCacheable(true).setVisitorFunction(new AddSharableVisitorFunction(className)).build());
        });

        /*
         * Transform ChannelHandlerAdapter to:
         *
         * public boolean isSharable() {
         * if (this instanceof NettySharable) {
         * return true;
         * }
         * return this.isSharable0();
         * }
         *
         * where `isSharable0` is the old `isSharable` method of ChannelHandlerAdapter
         */
        String classAdapterClassName = "io.netty.channel.ChannelHandlerAdapter";
        producer.produce(new BytecodeTransformerBuildItem.Builder().setClassToTransform(classAdapterClassName)
                .setCacheable(true).setVisitorFunction(new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        ClassTransformer transformer = new ClassTransformer(classAdapterClassName);

                        MethodDescriptor isSharableMethod = MethodDescriptor.ofMethod(classAdapterClassName, "isSharable",
                                boolean.class);

                        // old isSharable becomes isSharable0
                        transformer.modifyMethod(isSharableMethod).rename("isSharable0");

                        // new isSharable method
                        {
                            MethodDescriptor isSharable0Method = MethodDescriptor.ofMethod(classAdapterClassName, "isSharable0",
                                    boolean.class);

                            MethodCreator mc = transformer.addMethod(isSharableMethod);

                            // clazz instanceof NettySharable
                            ResultHandle isInstanceOf = mc.instanceOf(mc.getThis(), NettySharable.class);

                            // if (instanceof) return true; else call isSharable0
                            BytecodeCreator trueBranch = mc.ifNonZero(isInstanceOf).trueBranch();
                            trueBranch.returnValue(trueBranch.load(true));
                            ResultHandle result = mc.invokeVirtualMethod(isSharable0Method, mc.getThis());

                            mc.returnValue(result);
                        }

                        return transformer.applyTo(classVisitor);
                    }
                }).build());

    }

    private static void replaceWithReturnTrue(ClassTransformer transformer, String className, String methodName) {
        MethodDescriptor md = MethodDescriptor.ofMethod(className, methodName, boolean.class);
        transformer.removeMethod(md);
        MethodCreator m = transformer.addMethod(md).setModifiers(Modifier.STATIC);
        m.returnValue(m.load(true));
    }

    /**
     * Generates a private static helper method {@code readBitsMaxDirectMemory()} that returns the value of
     * {@code java.nio.Bits.MAX_MEMORY}. This is used by the clinit patcher to replace the
     * {@code BITS_MAX_DIRECT_MEMORY = -1} assignment in the {@code unsafe == null} branch, so that the value is
     * available even without Unsafe. This avoids the expensive {@code ManagementFactory.getRuntimeMXBean()} fallback
     * in {@code PlatformDependent.estimateMaxDirectMemory()}.
     * <p>
     * {@code Bits.MAX_MEMORY} is initialized from {@code jdk.internal.misc.VM.maxDirectMemory()}, so the generated
     * code calls that method directly: the {@code java.base/jdk.internal.misc} package is opened to the application by
     * the {@code Add-Opens} manifest entry Quarkus generates, and an open package is also exported. The direct call
     * avoids the reflective {@code Field.get()}, whose first use builds a method handle based field accessor, which
     * was the most expensive part of the Netty pre-initialization thread. If the package is not accessible (for
     * instance when the application is started without the manifest options), the method falls back to the reflective
     * read.
     */
    private static void generateReadBitsMaxDirectMemory(ClassTransformer transformer, String className) {
        MethodDescriptor md = MethodDescriptor.ofMethod(className, "readBitsMaxDirectMemory", long.class);
        MethodCreator m = transformer.addMethod(md).setModifiers(Modifier.STATIC | Modifier.PRIVATE);

        AssignableResultHandle result = m.createVariable(long.class);
        m.assign(result, m.load(-1L));

        // try { return jdk.internal.misc.VM.maxDirectMemory(); } catch (Throwable ignored) { }
        TryBlock directBlock = m.tryBlock();
        ResultHandle maxDirectMemory = directBlock.invokeStaticMethod(
                MethodDescriptor.ofMethod("jdk.internal.misc.VM", "maxDirectMemory", long.class));
        directBlock.returnValue(maxDirectMemory);
        directBlock.addCatch(Throwable.class);

        TryBlock tryBlock = m.tryBlock();

        ResultHandle bitsClass = tryBlock.invokeStaticMethod(
                MethodDescriptor.ofMethod(Class.class, "forName", Class.class,
                        String.class, boolean.class, ClassLoader.class),
                tryBlock.load("java.nio.Bits"),
                tryBlock.load(false),
                tryBlock.invokeStaticMethod(
                        MethodDescriptor.ofMethod(ClassLoader.class, "getSystemClassLoader", ClassLoader.class)));

        ResultHandle field = tryBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Class.class, "getDeclaredField",
                        java.lang.reflect.Field.class, String.class),
                bitsClass, tryBlock.load("MAX_MEMORY"));

        tryBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(java.lang.reflect.Field.class, "setAccessible",
                        void.class, boolean.class),
                field, tryBlock.load(true));

        ResultHandle value = tryBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(java.lang.reflect.Field.class, "get",
                        Object.class, Object.class),
                field, tryBlock.loadNull());

        ResultHandle longValue = tryBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Long.class, "longValue", long.class),
                value);

        tryBlock.assign(result, longValue);

        tryBlock.addCatch(Throwable.class);

        m.returnValue(result);
    }

    private static void generateDirectBufferAddress(ClassTransformer transformer, String className) {
        MethodDescriptor md = MethodDescriptor.ofMethod(className, "directBufferAddress",
                long.class, ByteBuffer.class);
        transformer.removeMethod(md);
        MethodCreator m = transformer.addMethod(md).setModifiers(Modifier.STATIC);

        ResultHandle buffer = m.getMethodParam(0);

        // if (hasUnsafe()) return getLong(buffer, ADDRESS_FIELD_OFFSET);
        ResultHandle hasUnsafe = m.invokeStaticMethod(
                MethodDescriptor.ofMethod(className, "hasUnsafe", boolean.class));
        BranchResult unsafeCheck = m.ifNonZero(hasUnsafe);

        BytecodeCreator unsafeBranch = unsafeCheck.trueBranch();
        ResultHandle addressFieldOffset = unsafeBranch.readStaticField(
                FieldDescriptor.of(className, "ADDRESS_FIELD_OFFSET", long.class));
        ResultHandle addr = unsafeBranch.invokeStaticMethod(
                MethodDescriptor.ofMethod(className, "getLong",
                        long.class, Object.class, long.class),
                buffer, addressFieldOffset);
        unsafeBranch.returnValue(addr);

        // return MemorySegment.ofBuffer((Buffer) buffer).address() - buffer.position();
        ResultHandle segment = m.invokeStaticInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.MemorySegment", "ofBuffer",
                        "java.lang.foreign.MemorySegment",
                        "java.nio.Buffer"),
                buffer);
        ResultHandle segAddr = m.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.MemorySegment", "address",
                        long.class),
                segment);
        ResultHandle position = m.invokeVirtualMethod(
                MethodDescriptor.ofMethod(ByteBuffer.class, "position", int.class),
                buffer);
        ResultHandle positionLong = m.convertPrimitive(position, long.class);
        ResultHandle result = m.subtract(segAddr, positionLong);
        m.returnValue(result);
    }

    /**
     * Creates an ASM {@link ClassVisitor} that intercepts the {@code <clinit>} method and patches
     * version-gated MethodHandle lookup blocks. For each block matching the pattern:
     *
     * <pre>
     * invokestatic javaVersion()I
     * bipush/sipush N
     * if_icmplt/if_icmple ELSE_LABEL
     * ... MethodHandle lookup via AccessController.doPrivileged ...
     * putstatic FIELD
     * goto END_LABEL
     * ELSE_LABEL:
     * aconst_null
     * putstatic FIELD
     * </pre>
     *
     * If {@code FIELD} is in {@code fieldsToSkip}, the version check is replaced with a
     * {@code GOTO ELSE_LABEL}, forcing the null assignment and skipping the expensive
     * MethodHandle lookup.
     * <p>
     * Also patches the {@code BITS_MAX_DIRECT_MEMORY = -1} assignment in the {@code unsafe == null}
     * branch to call {@code readBitsMaxDirectMemory()} instead, reading the value via reflection.
     */
    private static ClassVisitor createClinitPatcher(ClassVisitor downstream, Set<String> fieldsToSkip,
            Set<String> knownUnhandledFields) {
        return new ClassVisitor(Gizmo.ASM_API_VERSION, downstream) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if ("<clinit>".equals(name)) {
                    MethodVisitor target = mv;
                    return new MethodNode(Gizmo.ASM_API_VERSION, access, name, descriptor,
                            signature, exceptions) {
                        @Override
                        public void visitEnd() {
                            super.visitEnd();
                            patchClinitVersionChecks(this.instructions, fieldsToSkip,
                                    knownUnhandledFields);
                            this.accept(target);
                        }
                    };
                }
                return mv;
            }
        };
    }

    private static void patchClinitVersionChecks(
            org.objectweb.asm.tree.InsnList instructions, Set<String> fieldsToSkip,
            Set<String> knownUnhandledFields) {
        Set<String> patchedFields = new java.util.HashSet<>();
        AbstractInsnNode node = instructions.getFirst();
        while (node != null) {
            if (node instanceof MethodInsnNode methodInsn
                    && methodInsn.getOpcode() == Opcodes.INVOKESTATIC
                    && "javaVersion".equals(methodInsn.name)
                    && "io/netty/util/internal/PlatformDependent0".equals(methodInsn.owner)) {

                AbstractInsnNode pushNode = nextRealInsn(methodInsn);
                if (pushNode == null || !(pushNode instanceof IntInsnNode)) {
                    node = node.getNext();
                    continue;
                }

                AbstractInsnNode jumpNode = nextRealInsn(pushNode);
                if (jumpNode == null || !(jumpNode instanceof JumpInsnNode jumpInsn)) {
                    node = node.getNext();
                    continue;
                }

                int opcode = jumpInsn.getOpcode();
                if (opcode != Opcodes.IF_ICMPLT && opcode != Opcodes.IF_ICMPLE) {
                    node = node.getNext();
                    continue;
                }

                // Walk from the jump target to find the null-assignment and identify the field
                AbstractInsnNode nullInsn = nextRealInsn(jumpInsn.label);
                if (nullInsn == null || nullInsn.getOpcode() != Opcodes.ACONST_NULL) {
                    node = node.getNext();
                    continue;
                }

                AbstractInsnNode putStaticInsn = nextRealInsn(nullInsn);
                if (!(putStaticInsn instanceof FieldInsnNode fieldInsn)
                        || fieldInsn.getOpcode() != Opcodes.PUTSTATIC) {
                    node = node.getNext();
                    continue;
                }

                String fieldName = fieldInsn.name;

                if (!fieldsToSkip.contains(fieldName)) {
                    if (!knownUnhandledFields.contains(fieldName)) {
                        throw new IllegalStateException(
                                "PlatformDependent0.<clinit> contains an unhandled version-gated MethodHandle "
                                        + "lookup block for field '" + fieldName + "'. "
                                        + "This likely means Netty added a new version-gated block. "
                                        + "Either add it to fieldsToSkip (and replace the accessor method) "
                                        + "or add it to KNOWN_UNHANDLED_CLINIT_FIELDS.");
                    }
                    node = node.getNext();
                    continue;
                }

                // Advance past the nodes we're about to remove BEFORE removing them
                AbstractInsnNode nextNode = jumpInsn.getNext();

                // Patch: remove invokestatic + push constant, change conditional to GOTO
                instructions.remove(methodInsn);
                instructions.remove(pushNode);
                jumpInsn.setOpcode(Opcodes.GOTO);
                patchedFields.add(fieldName);

                node = nextNode;
                continue;
            }
            node = node.getNext();
        }

        // Verify that all expected fields were found and patched
        if (!patchedFields.equals(fieldsToSkip)) {
            Set<String> missing = new java.util.HashSet<>(fieldsToSkip);
            missing.removeAll(patchedFields);
            throw new IllegalStateException(
                    "Failed to patch all expected version-gated MethodHandle lookup blocks in "
                            + "PlatformDependent0.<clinit>. Missing fields: " + missing + ". "
                            + "The bytecode pattern may have changed in a Netty update.");
        }

        // Patch the BITS_MAX_DIRECT_MEMORY = -1 assignment in the unsafe == null branch.
        // Replace: ldc2_w -1L; putstatic BITS_MAX_DIRECT_MEMORY
        // With:    invokestatic readBitsMaxDirectMemory(); putstatic BITS_MAX_DIRECT_MEMORY
        // This reads java.nio.Bits.MAX_MEMORY via reflection so the value is available
        // even without Unsafe, avoiding the expensive ManagementFactory fallback in
        // PlatformDependent.estimateMaxDirectMemory().
        boolean patchedBitsMaxDirectMemory = false;
        for (AbstractInsnNode n = instructions.getFirst(); n != null; n = n.getNext()) {
            if (n instanceof org.objectweb.asm.tree.LdcInsnNode ldcInsn
                    && ldcInsn.cst instanceof Long longVal
                    && longVal == -1L) {
                AbstractInsnNode nextInsn = nextRealInsn(n);
                if (nextInsn instanceof FieldInsnNode fieldInsn
                        && fieldInsn.getOpcode() == Opcodes.PUTSTATIC
                        && "BITS_MAX_DIRECT_MEMORY".equals(fieldInsn.name)) {
                    instructions.set(n, new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "io/netty/util/internal/PlatformDependent0",
                            "readBitsMaxDirectMemory",
                            "()J",
                            false));
                    patchedBitsMaxDirectMemory = true;
                    break;
                }
            }
        }
        if (!patchedBitsMaxDirectMemory) {
            throw new IllegalStateException(
                    "Could not find BITS_MAX_DIRECT_MEMORY = -1 assignment in "
                            + "PlatformDependent0.<clinit>. "
                            + "The bytecode pattern may have changed in a Netty update.");
        }
    }

    private static AbstractInsnNode nextRealInsn(AbstractInsnNode node) {
        node = node.getNext();
        while (node != null) {
            int type = node.getType();
            if (type != AbstractInsnNode.LABEL && type != AbstractInsnNode.FRAME
                    && type != AbstractInsnNode.LINE) {
                return node;
            }
            node = node.getNext();
        }
        return null;
    }

    private static class AddSharableVisitorFunction implements BiFunction<String, ClassVisitor, ClassVisitor> {

        private final String className;

        private AddSharableVisitorFunction(String className) {
            this.className = className;
        }

        @Override
        public ClassVisitor apply(String s, ClassVisitor classVisitor) {
            ClassTransformer transformer = new ClassTransformer(className);
            transformer.addInterface(NettySharable.class);
            return transformer.applyTo(classVisitor);
        }
    }
}
