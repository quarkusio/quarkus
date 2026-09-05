package io.quarkus.netty.deployment;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
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
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
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
import io.quarkus.netty.runtime.MachineIdGenerator;
import io.quarkus.netty.runtime.NettyRecorder;
import io.quarkus.netty.runtime.NettySharable;
import io.quarkus.runtime.util.JavaVersionGreaterOrEqual25;

class NettyProcessor {

    private static final Logger log = Logger.getLogger(NettyProcessor.class);

    private static final int DEFAULT_NETTY_ALLOCATOR_MAX_ORDER = 3;

    static {
        InternalLoggerFactory.setDefaultFactory(new JBossNettyLoggerFactory());
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
    public SystemPropertyBuildItem limitArenaSize(NettyBuildTimeConfig config,
            List<MinNettyAllocatorMaxOrderBuildItem> minMaxOrderBuildItems) {
        String maxOrder = calculateMaxOrder(config.allocatorMaxOrder(), minMaxOrderBuildItems, true);

        //in native mode we limit the size of the epoll array
        //if the array overflows the selector just moves the overflow to a map
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
    @BuildStep(onlyIf = JavaVersionGreaterOrEqual25.class)
    NativeImageConfigBuildItem build25Specific(
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

        final NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                .addNativeImageSystemProperty("io.netty.tryReflectionSetAccessible", "true")
                .addNativeImageSystemProperty("io.netty.noUnsafe", "true");
        return builder.build();
    }

    @BuildStep
    NativeImageConfigBuildItem build(
            NettyBuildTimeConfig config,
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

        String maxOrder = calculateMaxOrder(config.allocatorMaxOrder(), minMaxOrderBuildItems, false);

        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                // Use small chunks to avoid a lot of wasted space. Default is 16mb * arenas (derived from core count)
                // Since buffers are cached to threads, the malloc overhead is temporary anyway
                .addNativeImageSystemProperty("io.netty.allocator.maxOrder", maxOrder)
                // Spotted with Netty 4.1.135.Final
                .addRuntimeInitializedClass("io.netty.internal.tcnative.SSL")
                // Runtime initialize to respect io.netty.handler.ssl.conscrypt.useBufferAllocator
                .addRuntimeInitializedClass("io.netty.handler.ssl.ConscryptAlpnSslEngine")
                .addRuntimeInitializedClass("io.netty.util.internal.CleanerJava24Linker")
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
            // Brotli is an optional dependency, we should only runtime initialize BrotliOptions to avoid
            // IllegalStateException when brotli (e.g. com.aayushatharva.brotli4j.Brotli4jLoader) is not available.
            // This will result in a runtime ClassNotFoundException if the user tries to use Brotli.
            // Due to https://github.com/quarkusio/quarkus/issues/43662 we cannot do this yet though so we always enable
            // runtime initialization of BrotliOptions if the class is present
            if (QuarkusClassLoader.isClassPresentAtRuntime("io.netty.handler.codec.compression.BrotliOptions")) {
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
                    // Runtime initialize due to dependency on Epoll and the use of AtomicIntegerFieldUpdater?
                    .addRuntimeInitializedClass("io.netty.channel.kqueue.KQueueEventLoop");
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
                .addRuntimeInitializedClass("io.netty.buffer.AbstractReferenceCountedByteBuf");

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
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerEventLoopBeans(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            Optional<EventLoopSupplierBuildItem> loopSupplierBuildItem,
            NettyRecorder recorder,
            BuildProducer<EventLoopGroupBuildItem> eventLoopGroups) {
        Supplier<EventLoopGroup> boss;
        Supplier<EventLoopGroup> main;
        if (loopSupplierBuildItem.isPresent()) {
            boss = (Supplier) loopSupplierBuildItem.get().getBossSupplier();
            main = (Supplier) loopSupplierBuildItem.get().getMainSupplier();
        } else {
            boss = recorder.createEventLoop(1);
            main = recorder.createEventLoop(0);
        }

        // IMPLEMENTATION NOTE:
        // We use Singleton scope for both beans. ApplicationScoped causes problems with EventLoopGroup.next()
        // which overrides the EventExecutorGroup.next() method but since Netty 4 is compiled with JDK6 the corresponding bridge method
        // is not generated and the invocation upon the client proxy results in an AbstractMethodError
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(EventLoopGroup.class)
                .supplier(boss)
                .scope(Singleton.class)
                .addQualifier(BossEventLoopGroup.class)
                .unremovable()
                .setRuntimeInit()
                .done());
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(EventLoopGroup.class)
                .supplier(main)
                .scope(Singleton.class)
                .addQualifier(MainEventLoopGroup.class)
                .unremovable()
                .setRuntimeInit()
                .done());

        eventLoopGroups.produce(new EventLoopGroupBuildItem(boss, main));
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

    private String calculateMaxOrder(OptionalInt userConfig, List<MinNettyAllocatorMaxOrderBuildItem> minMaxOrderBuildItems,
            boolean shouldWarn) {
        int result = DEFAULT_NETTY_ALLOCATOR_MAX_ORDER;
        for (MinNettyAllocatorMaxOrderBuildItem minMaxOrderBuildItem : minMaxOrderBuildItems) {
            if (minMaxOrderBuildItem.getMaxOrder() > result) {
                result = minMaxOrderBuildItem.getMaxOrder();
            }
        }

        if (userConfig.isPresent()) {
            int v = userConfig.getAsInt();
            if (result > v && shouldWarn) {
                log.warnf(
                        "The configuration set `quarkus.netty.allocator-max-order` to %d. This value is lower than the value requested by the extensions (%d). %d will be used anyway.",
                        v, result, v);

            }
            return Integer.toString(v);
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

                                // --- readBitsMaxDirectMemory() -> reads Bits.MAX_MEMORY via reflection ---
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
     * Rewrites {@code PlatformDependent#estimateMaxDirectMemory()} to replace the
     * reflection-based VM argument lookup with direct calls. The original code uses
     * {@code Class.forName} + {@code MethodHandles.publicLookup().findStatic/findVirtual}
     * to call {@code ManagementFactory.getRuntimeMXBean().getInputArguments()} because
     * "Android doesn't have these classes". Since we target JDK 17+, these are always available.
     * <p>
     * Using ASM's tree API, we surgically replace the instruction sequence that performs
     * the reflection with a direct invocation, preserving all surrounding code (the loop,
     * the regex matching, the switch, the logging).
     */
    @BuildStep
    void transformPlatformDependentEstimateMaxDirectMemory(BuildProducer<BytecodeTransformerBuildItem> producer) {
        String className = PlatformDependent.class.getName();

        producer.produce(new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(className)
                .setCacheable(true)
                .setClassReaderOptions(ClassReader.EXPAND_FRAMES)
                .setVisitorFunction(new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {
                                MethodVisitor mv = super.visitMethod(access, name, descriptor,
                                        signature, exceptions);
                                if ("estimateMaxDirectMemory".equals(name) && "()J".equals(descriptor)) {
                                    MethodVisitor target = mv;
                                    return new MethodNode(Gizmo.ASM_API_VERSION, access, name, descriptor,
                                            signature, exceptions) {
                                        @Override
                                        public void visitEnd() {
                                            super.visitEnd();
                                            patchEstimateMaxDirectMemory(this.instructions);
                                            this.accept(target);
                                        }
                                    };
                                }
                                return mv;
                            }
                        };
                    }
                })
                .build());
    }

    /**
     * Replaces the reflection-based sequence in {@code estimateMaxDirectMemory} with direct calls.
     * <p>
     * Finds and replaces:
     *
     * <pre>
     * invokestatic getSystemClassLoader -> astore X
     * ldc "java.lang.management.ManagementFactory"
     * ... Class.forName ...
     * ... Class.forName ...
     * ... MethodHandles.publicLookup ...
     * ... lookup.findStatic ...
     * ... lookup.findVirtual ...
     * ... getInputArguments.invoke(getRuntime.invoke()) ...
     * checkcast List -> astore Y (vmArgs)
     * </pre>
     *
     * with:
     *
     * <pre>
     * invokestatic ManagementFactory.getRuntimeMXBean()
     * invokeinterface RuntimeMXBean.getInputArguments()
     * astore Y (vmArgs)
     * </pre>
     *
     * The approach: find the {@code invokestatic getSystemClassLoader} that starts the block
     * and the {@code checkcast java/util/List} that ends it, remove everything in between,
     * and insert the direct calls before the checkcast (which becomes a no-op but is harmless).
     */
    private static void patchEstimateMaxDirectMemory(org.objectweb.asm.tree.InsnList instructions) {
        // Find the invokestatic getSystemClassLoader call that starts the reflection block
        AbstractInsnNode startNode = null;
        for (AbstractInsnNode node = instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof MethodInsnNode methodInsn
                    && methodInsn.getOpcode() == Opcodes.INVOKESTATIC
                    && "getSystemClassLoader".equals(methodInsn.name)) {
                startNode = node;
                break;
            }
        }
        if (startNode == null) {
            throw new IllegalStateException(
                    "Could not find getSystemClassLoader call in PlatformDependent.estimateMaxDirectMemory. "
                            + "The bytecode pattern may have changed in a Netty update.");
        }

        // Find the last MethodHandle.invoke call that ends the reflection block.
        // The sequence ends with: invokevirtual MethodHandle.invoke -> astore (vmArgs).
        // We keep the astore and replace everything before it.
        AbstractInsnNode lastInvoke = null;
        for (AbstractInsnNode node = startNode.getNext(); node != null; node = node.getNext()) {
            if (node instanceof MethodInsnNode methodInsn
                    && "invoke".equals(methodInsn.name)
                    && "java/lang/invoke/MethodHandle".equals(methodInsn.owner)) {
                lastInvoke = node;
            }
            // Stop scanning once we pass the reflection block (next invokestatic is
            // getMaxDirectMemorySizeArgPattern)
            if (node instanceof MethodInsnNode methodInsn
                    && "getMaxDirectMemorySizeArgPattern".equals(methodInsn.name)) {
                break;
            }
        }
        if (lastInvoke == null) {
            throw new IllegalStateException(
                    "Could not find MethodHandle.invoke in PlatformDependent.estimateMaxDirectMemory. "
                            + "The bytecode pattern may have changed in a Netty update.");
        }

        // The astore after the last invoke stores vmArgs - we keep it
        AbstractInsnNode astoreNode = nextRealInsn(lastInvoke);

        // Remove everything from startNode through lastInvoke (inclusive)
        AbstractInsnNode current = startNode;
        while (current != astoreNode) {
            AbstractInsnNode next = current.getNext();
            instructions.remove(current);
            current = next;
        }

        // Insert direct calls before the astore:
        // invokestatic ManagementFactory.getRuntimeMXBean() -> RuntimeMXBean
        // invokeinterface RuntimeMXBean.getInputArguments() -> List
        instructions.insertBefore(astoreNode, new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/lang/management/ManagementFactory",
                "getRuntimeMXBean",
                "()Ljava/lang/management/RuntimeMXBean;",
                false));
        instructions.insertBefore(astoreNode, new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/lang/management/RuntimeMXBean",
                "getInputArguments",
                "()Ljava/util/List;",
                true));
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

    /**
     * Rewrites {@code CleanerJava24Linker}'s static initializer and
     * {@code CleanableDirectBufferImpl}'s constructor to avoid the expensive reflection-based
     * FFM API lookups via {@code Class.forName} and {@code MethodHandles} chains. When building
     * on Java 25+, the Foreign Function &amp; Memory API is stable and can be called directly,
     * eliminating ~20 reflective lookups and MethodHandle chain constructions.
     * <p>
     * {@code INVOKE_MALLOC} and {@code INVOKE_FREE} must remain {@code MethodHandle}s because
     * FFM's {@code Linker.downcallHandle()} always returns a {@code MethodHandle} to bridge
     * Java to native code. But {@code INVOKE_CREATE_BYTEBUFFER} wraps plain Java methods
     * ({@code MemorySegment.ofAddress/reinterpret/asByteBuffer}) and can be replaced with
     * direct calls in the inner class constructor.
     * <p>
     * We replace the static initializer with:
     *
     * <pre>{@code
     * static {
     *     logger = InternalLoggerFactory.getInstance(CleanerJava24Linker.class);
     *     MethodHandle mallocMethod = null;
     *     MethodHandle freeMethod = null;
     *     Throwable error = null;
     *     try {
     *         if (!CleanerJava24Linker.class.getModule().isNativeAccessEnabled()) {
     *             throw new UnsupportedOperationException(
     *                     "Native access (restricted methods) is not enabled for the io.netty.common module.");
     *         }
     *         if (ValueLayout.ADDRESS.byteSize() != Long.BYTES) {
     *             throw new UnsupportedOperationException(
     *                     "Linking to malloc and free is only supported on 64-bit platforms.");
     *         }
     *         Linker linker = Linker.nativeLinker();
     *         SymbolLookup defaultLookup = linker.defaultLookup();
     *         ValueLayout.OfLong javaLong = ValueLayout.JAVA_LONG;
     *         MemoryLayout[] layouts = new MemoryLayout[] { javaLong };
     *         Linker.Option[] emptyOptions = new Linker.Option[0];
     *         MemorySegment mallocPtr = defaultLookup.findOrThrow("malloc");
     *         mallocMethod = linker.downcallHandle(mallocPtr,
     *                 FunctionDescriptor.of(javaLong, layouts), emptyOptions);
     *         MemorySegment freePtr = defaultLookup.findOrThrow("free");
     *         freeMethod = linker.downcallHandle(freePtr,
     *                 FunctionDescriptor.ofVoid(layouts), emptyOptions);
     *     } catch (Throwable throwable) {
     *         mallocMethod = null;
     *         freeMethod = null;
     *         error = throwable;
     *     }
     *     if (error == null) {
     *         logger.debug("java.nio.ByteBuffer.cleaner(): available");
     *     } else {
     *         logger.debug("java.nio.ByteBuffer.cleaner(): unavailable", error);
     *     }
     *     INVOKE_MALLOC = mallocMethod;
     *     INVOKE_CREATE_BYTEBUFFER = null;
     *     INVOKE_FREE = freeMethod;
     * }
     * }</pre>
     *
     * And we replace the {@code INVOKE_CREATE_BYTEBUFFER.invokeExact(addr, (long) capacity)}
     * call in {@code CleanableDirectBufferImpl}'s constructor with:
     *
     * <pre>{@code
     * buffer = MemorySegment.ofAddress(addr).reinterpret((long) capacity).asByteBuffer();
     * }</pre>
     */
    @BuildStep
    void transformCleanerJava24Linker(CompiledJavaVersionBuildItem compiledJavaVersion,
            BuildProducer<BytecodeTransformerBuildItem> producer) {
        if (compiledJavaVersion.getJavaVersion()
                .isJava25OrHigher() != CompiledJavaVersionBuildItem.JavaVersion.Status.TRUE) {
            return;
        }

        String className = "io.netty.util.internal.CleanerJava24Linker";
        String innerClassName = className + "$CleanableDirectBufferImpl";

        // Transform the clinit of CleanerJava24Linker
        producer.produce(new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(className)
                .setCacheable(true)
                .setVisitorFunction(new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        ClassVisitor updateBytecodeVersion = new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                            @Override
                            public void visit(int version, int access, String name, String signature,
                                    String superName, String[] interfaces) {
                                super.visit(Math.max(version, 69), access, name, signature, superName, interfaces);
                            }
                        };

                        ClassTransformer transformer = new ClassTransformer(className);

                        MethodDescriptor clinitDescriptor = MethodDescriptor.ofMethod(
                                className, "<clinit>", void.class);
                        transformer.removeMethod(clinitDescriptor);

                        MethodCreator clinit = transformer.addMethod(clinitDescriptor)
                                .setModifiers(Modifier.STATIC);

                        generateCleanerJava24LinkerClinit(clinit, className);

                        return transformer.applyTo(updateBytecodeVersion);
                    }
                })
                .build());

        // Transform the constructor of CleanableDirectBufferImpl to replace
        // INVOKE_CREATE_BYTEBUFFER.invokeExact(addr, (long) capacity)
        // with MemorySegment.ofAddress(addr).reinterpret((long) capacity).asByteBuffer()
        producer.produce(new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(innerClassName)
                .setCacheable(true)
                .setVisitorFunction(new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        ClassVisitor updateBytecodeVersion = new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                            @Override
                            public void visit(int version, int access, String name, String signature,
                                    String superName, String[] interfaces) {
                                super.visit(Math.max(version, 69), access, name, signature, superName, interfaces);
                            }
                        };

                        ClassTransformer transformer = new ClassTransformer(innerClassName);

                        MethodDescriptor ctorDescriptor = MethodDescriptor.ofConstructor(
                                innerClassName, int.class);
                        transformer.removeMethod(ctorDescriptor);

                        MethodCreator ctor = transformer.addMethod(ctorDescriptor)
                                .setModifiers(Modifier.PRIVATE);

                        generateCleanableDirectBufferImplCtor(ctor, className, innerClassName);

                        return transformer.applyTo(updateBytecodeVersion);
                    }
                })
                .build());
    }

    private static void generateCleanerJava24LinkerClinit(MethodCreator clinit, String className) {
        // Field descriptors for the static fields we need to initialize
        FieldDescriptor loggerField = FieldDescriptor.of(className, "logger",
                "io.netty.util.internal.logging.InternalLogger");
        FieldDescriptor mallocField = FieldDescriptor.of(className, "INVOKE_MALLOC",
                MethodHandle.class);
        FieldDescriptor wrapField = FieldDescriptor.of(className, "INVOKE_CREATE_BYTEBUFFER",
                MethodHandle.class);
        FieldDescriptor freeField = FieldDescriptor.of(className, "INVOKE_FREE",
                MethodHandle.class);

        // logger = InternalLoggerFactory.getInstance(CleanerJava24Linker.class)
        ResultHandle clazz = clinit.loadClassFromTCCL(className);
        ResultHandle loggerValue = clinit.invokeStaticMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.logging.InternalLoggerFactory",
                        "getInstance", "io.netty.util.internal.logging.InternalLogger", Class.class),
                clazz);
        clinit.writeStaticField(loggerField, loggerValue);

        // Local variables for the MethodHandle fields and error tracking
        AssignableResultHandle mallocVar = clinit.createVariable(MethodHandle.class);
        AssignableResultHandle freeVar = clinit.createVariable(MethodHandle.class);
        AssignableResultHandle errorVar = clinit.createVariable(Throwable.class);
        clinit.assign(mallocVar, clinit.loadNull());
        clinit.assign(freeVar, clinit.loadNull());
        clinit.assign(errorVar, clinit.loadNull());

        TryBlock tryBlock = clinit.tryBlock();

        // Check native access: CleanerJava24Linker.class.getModule().isNativeAccessEnabled()
        ResultHandle classRef = tryBlock.loadClassFromTCCL(className);
        ResultHandle module = tryBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Class.class, "getModule", Module.class), classRef);
        ResultHandle isNativeAccess = tryBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Module.class, "isNativeAccessEnabled", boolean.class), module);

        BranchResult nativeCheck = tryBlock.ifNonZero(isNativeAccess);

        // No native access: throw UnsupportedOperationException
        BytecodeCreator noNative = nativeCheck.falseBranch();
        noNative.throwException(noNative.newInstance(
                MethodDescriptor.ofConstructor(UnsupportedOperationException.class, String.class),
                noNative.load(
                        "Native access (restricted methods) is not enabled for the io.netty.common module.")));

        // Has native access: check 64-bit platform
        BytecodeCreator hasNative = nativeCheck.trueBranch();

        // ValueLayout.ADDRESS.byteSize() != Long.BYTES
        ResultHandle addressLayout = hasNative.readStaticField(
                FieldDescriptor.of("java.lang.foreign.ValueLayout", "ADDRESS",
                        "java.lang.foreign.AddressLayout"));
        ResultHandle addressSize = hasNative.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.MemoryLayout", "byteSize", long.class),
                addressLayout);
        ResultHandle cmp = hasNative.invokeStaticMethod(
                MethodDescriptor.ofMethod(Long.class, "compare", int.class, long.class, long.class),
                addressSize, hasNative.load(8L));
        BranchResult sizeCheck = hasNative.ifNonZero(cmp);

        // Not 64-bit: throw
        BytecodeCreator not64bit = sizeCheck.trueBranch();
        not64bit.throwException(not64bit.newInstance(
                MethodDescriptor.ofConstructor(UnsupportedOperationException.class, String.class),
                not64bit.load("Linking to malloc and free is only supported on 64-bit platforms.")));

        // 64-bit: create the downcall MethodHandles for malloc and free
        BytecodeCreator bc = sizeCheck.falseBranch();

        // Linker linker = Linker.nativeLinker()
        ResultHandle linker = bc.invokeStaticInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.Linker", "nativeLinker",
                        "java.lang.foreign.Linker"));

        // SymbolLookup defaultLookup = linker.defaultLookup()
        ResultHandle defaultLookup = bc.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.Linker", "defaultLookup",
                        "java.lang.foreign.SymbolLookup"),
                linker);

        // ValueLayout.OfLong javaLong = ValueLayout.JAVA_LONG
        ResultHandle javaLong = bc.readStaticField(
                FieldDescriptor.of("java.lang.foreign.ValueLayout", "JAVA_LONG",
                        "java.lang.foreign.ValueLayout$OfLong"));

        // MemoryLayout[] layouts = new MemoryLayout[] { javaLong }
        ResultHandle layoutArray = bc.newArray("java.lang.foreign.MemoryLayout", 1);
        bc.writeArrayValue(layoutArray, 0, javaLong);

        // Linker.Option[] emptyOptions = new Linker.Option[0]
        ResultHandle emptyOptions = bc.newArray("java.lang.foreign.Linker$Option", 0);

        // --- malloc ---
        ResultHandle mallocPtr = bc.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.SymbolLookup", "findOrThrow",
                        "java.lang.foreign.MemorySegment", String.class),
                defaultLookup, bc.load("malloc"));
        ResultHandle mallocDesc = bc.invokeStaticInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.FunctionDescriptor", "of",
                        "java.lang.foreign.FunctionDescriptor",
                        "java.lang.foreign.MemoryLayout", "[Ljava.lang.foreign.MemoryLayout;"),
                javaLong, layoutArray);
        ResultHandle mallocHandle = bc.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.Linker", "downcallHandle",
                        MethodHandle.class,
                        "java.lang.foreign.MemorySegment", "java.lang.foreign.FunctionDescriptor",
                        "[Ljava.lang.foreign.Linker$Option;"),
                linker, mallocPtr, mallocDesc, emptyOptions);
        bc.assign(mallocVar, mallocHandle);

        // --- free ---
        ResultHandle freePtr = bc.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.SymbolLookup", "findOrThrow",
                        "java.lang.foreign.MemorySegment", String.class),
                defaultLookup, bc.load("free"));
        ResultHandle freeDesc = bc.invokeStaticInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.FunctionDescriptor", "ofVoid",
                        "java.lang.foreign.FunctionDescriptor",
                        "[Ljava.lang.foreign.MemoryLayout;"),
                layoutArray);
        ResultHandle freeHandle = bc.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.Linker", "downcallHandle",
                        MethodHandle.class,
                        "java.lang.foreign.MemorySegment", "java.lang.foreign.FunctionDescriptor",
                        "[Ljava.lang.foreign.Linker$Option;"),
                linker, freePtr, freeDesc, emptyOptions);
        bc.assign(freeVar, freeHandle);

        // Catch block: null out everything and save error
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        catchBlock.assign(mallocVar, catchBlock.loadNull());
        catchBlock.assign(freeVar, catchBlock.loadNull());
        catchBlock.assign(errorVar, catchBlock.getCaughtException());

        // After try-catch: log result and assign to static fields
        ResultHandle loggerRef = clinit.readStaticField(loggerField);
        BranchResult errorCheck = clinit.ifNull(errorVar);

        // No error: logger.debug("java.nio.ByteBuffer.cleaner(): available")
        BytecodeCreator noError = errorCheck.trueBranch();
        noError.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.logging.InternalLogger",
                        "debug", void.class, String.class),
                loggerRef, noError.load("java.nio.ByteBuffer.cleaner(): available"));

        // Has error: logger.debug("java.nio.ByteBuffer.cleaner(): unavailable", error)
        BytecodeCreator hasError = errorCheck.falseBranch();
        hasError.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.logging.InternalLogger",
                        "debug", void.class, String.class, Throwable.class),
                loggerRef, hasError.load("java.nio.ByteBuffer.cleaner(): unavailable"), errorVar);

        // Assign to static final fields
        clinit.writeStaticField(mallocField, mallocVar);
        // INVOKE_CREATE_BYTEBUFFER is no longer used - direct calls in CleanableDirectBufferImpl
        clinit.writeStaticField(wrapField, clinit.loadNull());
        clinit.writeStaticField(freeField, freeVar);

        clinit.returnVoid();
    }

    /**
     * Generates the constructor for {@code CleanableDirectBufferImpl} that replaces
     * {@code INVOKE_CREATE_BYTEBUFFER.invokeExact(addr, (long) capacity)} with direct
     * FFM calls.
     * <p>
     * The original constructor:
     *
     * <pre>{@code
     * private CleanableDirectBufferImpl(int capacity) {
     *     PlatformDependent.incrementMemoryCounter(capacity);
     *     long addr;
     *     try {
     *         addr = malloc(capacity);
     *     } catch (Throwable e) {
     *         PlatformDependent.decrementMemoryCounter(capacity);
     *         throw e;
     *     }
     *     try {
     *         memoryAddress = addr;
     *         buffer = (ByteBuffer) INVOKE_CREATE_BYTEBUFFER.invokeExact(addr, (long) capacity);
     *     } catch (Throwable throwable) {
     *         PlatformDependent.decrementMemoryCounter(capacity);
     *         Error error = new Error(throwable);
     *         try {
     *             free(addr);
     *         } catch (Throwable e) {
     *             error.addSuppressed(e);
     *         }
     *         throw error;
     *     }
     * }
     * }</pre>
     *
     * We replace the buffer assignment with:
     *
     * <pre>{@code
     * buffer = MemorySegment.ofAddress(addr).reinterpret((long) capacity).asByteBuffer();
     * }</pre>
     */
    private static void generateCleanableDirectBufferImplCtor(MethodCreator ctor,
            String outerClassName, String innerClassName) {
        FieldDescriptor bufferField = FieldDescriptor.of(innerClassName, "buffer",
                "java.nio.ByteBuffer");
        FieldDescriptor memoryAddressField = FieldDescriptor.of(innerClassName, "memoryAddress",
                long.class);

        ResultHandle self = ctor.getThis();
        ResultHandle capacity = ctor.getMethodParam(0);

        // super()
        ctor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(Object.class), self);

        // PlatformDependent.incrementMemoryCounter(capacity)
        ctor.invokeStaticMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.PlatformDependent",
                        "incrementMemoryCounter", void.class, int.class),
                capacity);

        // First try: addr = malloc(capacity)
        AssignableResultHandle addrVar = ctor.createVariable(long.class);
        TryBlock tryMalloc = ctor.tryBlock();
        ResultHandle addr = tryMalloc.invokeStaticMethod(
                MethodDescriptor.ofMethod(outerClassName, "malloc", long.class, int.class),
                capacity);
        tryMalloc.assign(addrVar, addr);

        CatchBlockCreator catchMalloc = tryMalloc.addCatch(Throwable.class);
        catchMalloc.invokeStaticMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.PlatformDependent",
                        "decrementMemoryCounter", void.class, int.class),
                capacity);
        catchMalloc.throwException(catchMalloc.getCaughtException());

        // Second try: set fields and create ByteBuffer
        TryBlock tryWrap = ctor.tryBlock();

        // memoryAddress = addr
        tryWrap.writeInstanceField(memoryAddressField, self, addrVar);

        // buffer = MemorySegment.ofAddress(addr).reinterpret((long) capacity).asByteBuffer()
        ResultHandle segment = tryWrap.invokeStaticInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.MemorySegment", "ofAddress",
                        "java.lang.foreign.MemorySegment", long.class),
                addrVar);
        ResultHandle capacityLong = tryWrap.convertPrimitive(capacity, long.class);
        ResultHandle reinterpreted = tryWrap.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.MemorySegment", "reinterpret",
                        "java.lang.foreign.MemorySegment", long.class),
                segment, capacityLong);
        ResultHandle byteBuffer = tryWrap.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.MemorySegment", "asByteBuffer",
                        "java.nio.ByteBuffer"),
                reinterpreted);
        tryWrap.writeInstanceField(bufferField, self, byteBuffer);

        // Catch block for the wrap try
        CatchBlockCreator catchWrap = tryWrap.addCatch(Throwable.class);
        catchWrap.invokeStaticMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.PlatformDependent",
                        "decrementMemoryCounter", void.class, int.class),
                capacity);
        ResultHandle error = catchWrap.newInstance(
                MethodDescriptor.ofConstructor(Error.class, Throwable.class),
                catchWrap.getCaughtException());

        // Inner try: free(addr) with suppressed exception handling
        TryBlock tryFree = catchWrap.tryBlock();
        tryFree.invokeStaticMethod(
                MethodDescriptor.ofMethod(outerClassName, "free", void.class, long.class),
                addrVar);
        CatchBlockCreator catchFree = tryFree.addCatch(Throwable.class);
        catchFree.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Throwable.class, "addSuppressed", void.class, Throwable.class),
                error, catchFree.getCaughtException());

        catchWrap.throwException(error);

        ctor.returnVoid();
    }

    /**
     * Rewrites {@code CleanerJava25}'s static initializer and {@code allocate(int)} method
     * to avoid the expensive reflection-based FFM API lookups via {@code Class.forName} and
     * complex {@code MethodHandles} composition chains (~10 MethodHandle operations). When
     * building on Java 25+, the Foreign Function &amp; Memory API is stable and can be called
     * directly.
     * <p>
     * The original clinit constructs a single {@code INVOKE_ALLOCATOR} MethodHandle that
     * chains Arena.ofShared(), allocate(), asByteBuffer(), address(), and the
     * CleanableDirectBufferImpl constructor into one composite handle. We replace all of that
     * with a simple Arena.ofShared() availability check.
     * <p>
     * We replace the static initializer with:
     *
     * <pre>{@code
     * static {
     *     logger = InternalLoggerFactory.getInstance(CleanerJava25.class);
     *     MethodHandle method = null;
     *     Throwable error = null;
     *     try {
     *         Arena arena = Arena.ofShared();
     *         arena.close();
     *         method = MethodHandles.identity(int.class);
     *     } catch (Throwable throwable) {
     *         error = throwable;
     *     }
     *     if (error == null) {
     *         logger.debug("java.nio.ByteBuffer.cleaner(): available");
     *     } else {
     *         logger.debug("java.nio.ByteBuffer.cleaner(): unavailable", error);
     *     }
     *     INVOKE_ALLOCATOR = method;
     * }
     * }</pre>
     *
     * And we replace the {@code allocate(int)} method with direct FFM calls:
     *
     * <pre>{@code
     * public CleanableDirectBuffer allocate(int capacity) {
     *     PlatformDependent.incrementMemoryCounter(capacity);
     *     try {
     *         Arena arena = Arena.ofShared();
     *         MemorySegment segment = arena.allocate((long) capacity);
     *         return new CleanableDirectBufferImpl(
     *                 (AutoCloseable) arena, segment.asByteBuffer(), segment.address());
     *     } catch (RuntimeException e) {
     *         PlatformDependent.decrementMemoryCounter(capacity);
     *         throw e;
     *     } catch (Throwable e) {
     *         PlatformDependent.decrementMemoryCounter(capacity);
     *         throw new IllegalStateException("Unexpected allocation exception", e);
     *     }
     * }
     * }</pre>
     */
    @BuildStep
    void transformCleanerJava25(CompiledJavaVersionBuildItem compiledJavaVersion,
            BuildProducer<BytecodeTransformerBuildItem> producer) {
        if (compiledJavaVersion.getJavaVersion()
                .isJava25OrHigher() != CompiledJavaVersionBuildItem.JavaVersion.Status.TRUE) {
            return;
        }

        String className = "io.netty.util.internal.CleanerJava25";
        String innerClassName = className + "$CleanableDirectBufferImpl";

        // Transform the clinit of CleanerJava25
        producer.produce(new BytecodeTransformerBuildItem.Builder()
                .setClassToTransform(className)
                .setCacheable(true)
                .setVisitorFunction(new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        ClassVisitor updateBytecodeVersion = new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                            @Override
                            public void visit(int version, int access, String name, String signature,
                                    String superName, String[] interfaces) {
                                super.visit(Math.max(version, 69), access, name, signature, superName, interfaces);
                            }
                        };

                        ClassTransformer transformer = new ClassTransformer(className);

                        // Replace <clinit>
                        MethodDescriptor clinitDescriptor = MethodDescriptor.ofMethod(
                                className, "<clinit>", void.class);
                        transformer.removeMethod(clinitDescriptor);
                        MethodCreator clinit = transformer.addMethod(clinitDescriptor)
                                .setModifiers(Modifier.STATIC);
                        generateCleanerJava25Clinit(clinit, className);

                        // Replace allocate(int)
                        MethodDescriptor allocateDescriptor = MethodDescriptor.ofMethod(
                                className, "allocate",
                                "io.netty.util.internal.CleanableDirectBuffer", int.class);
                        transformer.removeMethod(allocateDescriptor);
                        MethodCreator allocate = transformer.addMethod(allocateDescriptor)
                                .setModifiers(Modifier.PUBLIC);
                        generateCleanerJava25Allocate(allocate, className, innerClassName);

                        return transformer.applyTo(updateBytecodeVersion);
                    }
                })
                .build());
    }

    private static void generateCleanerJava25Clinit(MethodCreator clinit, String className) {
        FieldDescriptor loggerField = FieldDescriptor.of(className, "logger",
                "io.netty.util.internal.logging.InternalLogger");
        FieldDescriptor allocatorField = FieldDescriptor.of(className, "INVOKE_ALLOCATOR",
                MethodHandle.class);

        // logger = InternalLoggerFactory.getInstance(CleanerJava25.class)
        ResultHandle clazz = clinit.loadClassFromTCCL(className);
        ResultHandle loggerValue = clinit.invokeStaticMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.logging.InternalLoggerFactory",
                        "getInstance", "io.netty.util.internal.logging.InternalLogger", Class.class),
                clazz);
        clinit.writeStaticField(loggerField, loggerValue);

        AssignableResultHandle methodVar = clinit.createVariable(MethodHandle.class);
        AssignableResultHandle errorVar = clinit.createVariable(Throwable.class);
        clinit.assign(methodVar, clinit.loadNull());
        clinit.assign(errorVar, clinit.loadNull());

        TryBlock tryBlock = clinit.tryBlock();

        // Arena arena = Arena.ofShared()
        ResultHandle arena = tryBlock.invokeStaticInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.Arena", "ofShared",
                        "java.lang.foreign.Arena"));

        // arena.close() - verify it works (can fail on GraalVM 25.0.0)
        tryBlock.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.Arena", "close", void.class),
                arena);

        // Set to non-null marker for isSupported() check
        // method = MethodHandles.identity(int.class)
        ResultHandle intClass = tryBlock.readStaticField(
                FieldDescriptor.of(Integer.class, "TYPE", Class.class));
        ResultHandle marker = tryBlock.invokeStaticMethod(
                MethodDescriptor.ofMethod("java.lang.invoke.MethodHandles", "identity",
                        MethodHandle.class, Class.class),
                intClass);
        tryBlock.assign(methodVar, marker);

        // Catch block
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        catchBlock.assign(errorVar, catchBlock.getCaughtException());

        // Log result
        ResultHandle loggerRef = clinit.readStaticField(loggerField);
        BranchResult errorCheck = clinit.ifNull(errorVar);

        BytecodeCreator noError = errorCheck.trueBranch();
        noError.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.logging.InternalLogger",
                        "debug", void.class, String.class),
                loggerRef, noError.load("java.nio.ByteBuffer.cleaner(): available"));

        BytecodeCreator hasError = errorCheck.falseBranch();
        hasError.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.logging.InternalLogger",
                        "debug", void.class, String.class, Throwable.class),
                loggerRef, hasError.load("java.nio.ByteBuffer.cleaner(): unavailable"), errorVar);

        clinit.writeStaticField(allocatorField, methodVar);
        clinit.returnVoid();
    }

    private static void generateCleanerJava25Allocate(MethodCreator method,
            String className, String innerClassName) {
        ResultHandle capacity = method.getMethodParam(0);

        // PlatformDependent.incrementMemoryCounter(capacity)
        method.invokeStaticMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.PlatformDependent",
                        "incrementMemoryCounter", void.class, int.class),
                capacity);

        TryBlock tryBlock = method.tryBlock();

        // Arena arena = Arena.ofShared()
        ResultHandle arena = tryBlock.invokeStaticInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.Arena", "ofShared",
                        "java.lang.foreign.Arena"));

        // MemorySegment segment = arena.allocate((long) capacity)
        ResultHandle capacityLong = tryBlock.convertPrimitive(capacity, long.class);
        ResultHandle segment = tryBlock.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.Arena", "allocate",
                        "java.lang.foreign.MemorySegment", long.class),
                arena, capacityLong);

        // segment.asByteBuffer()
        ResultHandle byteBuffer = tryBlock.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.MemorySegment", "asByteBuffer",
                        "java.nio.ByteBuffer"),
                segment);

        // segment.address()
        ResultHandle address = tryBlock.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java.lang.foreign.MemorySegment", "address",
                        long.class),
                segment);

        // return new CleanableDirectBufferImpl(arena, byteBuffer, address)
        ResultHandle result = tryBlock.newInstance(
                MethodDescriptor.ofConstructor(innerClassName,
                        AutoCloseable.class, "java.nio.ByteBuffer", long.class),
                arena, byteBuffer, address);
        tryBlock.returnValue(result);

        // catch (RuntimeException e) { decrementMemoryCounter(capacity); throw e; }
        CatchBlockCreator catchRE = tryBlock.addCatch(RuntimeException.class);
        catchRE.invokeStaticMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.PlatformDependent",
                        "decrementMemoryCounter", void.class, int.class),
                capacity);
        catchRE.throwException(catchRE.getCaughtException());

        // catch (Throwable e) { decrementMemoryCounter(capacity); throw new IllegalStateException(..., e); }
        CatchBlockCreator catchT = tryBlock.addCatch(Throwable.class);
        catchT.invokeStaticMethod(
                MethodDescriptor.ofMethod("io.netty.util.internal.PlatformDependent",
                        "decrementMemoryCounter", void.class, int.class),
                capacity);
        ResultHandle ise = catchT.newInstance(
                MethodDescriptor.ofConstructor(IllegalStateException.class, String.class, Throwable.class),
                catchT.load("Unexpected allocation exception"), catchT.getCaughtException());
        catchT.throwException(ise);
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
    @Record(ExecutionTime.STATIC_INIT)
    void transformIsSharable(CombinedIndexBuildItem indexBuildItem,
            NettyRecorder recorder,
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
     * Generates a private static helper method {@code readBitsMaxDirectMemory()} that reads
     * {@code java.nio.Bits.MAX_MEMORY} via reflection. This is used by the clinit patcher to
     * replace the {@code BITS_MAX_DIRECT_MEMORY = -1} assignment in the {@code unsafe == null}
     * branch, so that the value is available even without Unsafe. This avoids the expensive
     * {@code ManagementFactory.getRuntimeMXBean()} fallback in
     * {@code PlatformDependent.estimateMaxDirectMemory()}.
     */
    private static void generateReadBitsMaxDirectMemory(ClassTransformer transformer, String className) {
        MethodDescriptor md = MethodDescriptor.ofMethod(className, "readBitsMaxDirectMemory", long.class);
        MethodCreator m = transformer.addMethod(md).setModifiers(Modifier.STATIC | Modifier.PRIVATE);

        AssignableResultHandle result = m.createVariable(long.class);
        m.assign(result, m.load(-1L));

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
