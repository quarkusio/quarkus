package io.quarkus.netty.deployment;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import io.netty.channel.EventLoopGroup;
import io.netty.resolver.dns.DnsServerAddressStreamProviders;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedRuntimeSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsafeAccessedFieldBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.netty.BossEventLoopGroup;
import io.quarkus.netty.MainEventLoopGroup;
import io.quarkus.netty.runtime.EmptyByteBufStub;
import io.quarkus.netty.runtime.MachineIdGenerator;
import io.quarkus.netty.runtime.NettyRecorder;
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
                .addNativeImageSystemProperty("io.netty.noUnsafe", "false");
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
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder");
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
     * Transforms {@code io.netty.util.internal.CleanerJava9} to take advantage of the fact that we know we are in Java 17+
     * Generated bytecode structure:
     *
     * <pre>{@code
     * final class CleanerJava9 implements Cleaner {
     *     private static final InternalLogger logger = InternalLoggerFactory.getInstance(CleanerJava9.class);
     *     private static final boolean SUPPORTED;
     *
     *     private static void freeDirectBufferPrivileged(ByteBuffer buffer) {
     *         Exception error = AccessController.doPrivileged(new PrivilegedAction(buffer));
     *         if (error != null) {
     *             PlatformDependent0.throwException(error);
     *         }
     *     }
     *
     *     static {
     *         boolean var2 = false;
     *         Throwable var3 = null;
     *         if (!PlatformDependent0.hasUnsafe()) {
     *             var3 = new UnsupportedOperationException("sun.misc.Unsafe unavailable");
     *         } else {
     *             try {
     *                 ByteBuffer var0 = ByteBuffer.allocateDirect(1);
     *                 PlatformDependent0.UNSAFE.invokeCleaner(var0);
     *                 var2 = true;
     *             } catch (Throwable var4) {
     *                 var3 = var4;
     *             }
     *         }
     *
     *         SUPPORTED = var2;
     *         if (var3 != null) {
     *             logger.debug("java.nio.ByteBuffer.cleaner(): unavailable", var3);
     *         } else {
     *             logger.debug("java.nio.ByteBuffer.cleaner(): available");
     *         }
     *     }
     *
     *     public void freeDirectBuffer(ByteBuffer var1) {
     *         if (System.getSecurityManager() != null) {
     *             freeDirectBufferPrivileged(var1);
     *         } else {
     *             PlatformDependent0.UNSAFE.invokeCleaner(var1);
     *         }
     *     }
     *
     *     public static boolean isSupported() {
     *         return SUPPORTED;
     *     }
     * }
     * }</pre>
     */
    @BuildStep
    BytecodeTransformerBuildItem transformCleanerJava9() {
        String className = "io.netty.util.internal.CleanerJava9";
        return new BytecodeTransformerBuildItem.Builder().setClassToTransform(className)
                .setCacheable(true).setVisitorFunction(
                        new BiFunction<>() {
                            @Override
                            public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                                FieldDescriptor supportedFieldDescriptor = FieldDescriptor
                                        .of(className, "SUPPORTED", boolean.class);

                                ClassTransformer transformer = new ClassTransformer(className);

                                transformer.addField(supportedFieldDescriptor)
                                        .setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL);
                                transformer.removeField("INVOKE_CLEANER", Method.class);

                                {
                                    MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(className, "<clinit>",
                                            void.class);
                                    transformer.removeMethod(methodDescriptor);

                                    {
                                        MethodCreator clinitMethod = transformer.addMethod(methodDescriptor).setModifiers(
                                                Modifier.PUBLIC | Modifier.STATIC);

                                        // Initialize logger
                                        ResultHandle cleanerClass = clinitMethod.loadClass(className);
                                        ResultHandle loggerInstance = clinitMethod.invokeStaticMethod(
                                                MethodDescriptor.ofMethod(
                                                        "io.netty.util.internal.logging.InternalLoggerFactory",
                                                        "getInstance",
                                                        InternalLogger.class.getName(),
                                                        Class.class),
                                                cleanerClass);
                                        FieldDescriptor loggerFieldDescriptor = FieldDescriptor.of(
                                                className, "logger",
                                                InternalLogger.class.getName());
                                        clinitMethod.writeStaticField(loggerFieldDescriptor, loggerInstance);

                                        // Initialize SUPPORTED
                                        AssignableResultHandle supportedVar = clinitMethod.createVariable(boolean.class);
                                        clinitMethod.assign(supportedVar, clinitMethod.load(false));
                                        AssignableResultHandle errorVar = clinitMethod.createVariable(Throwable.class);
                                        clinitMethod.assign(errorVar, clinitMethod.loadNull());

                                        // Check if Unsafe is available
                                        ResultHandle hasUnsafe = clinitMethod.invokeStaticMethod(
                                                MethodDescriptor.ofMethod("io.netty.util.internal.PlatformDependent0",
                                                        "hasUnsafe",
                                                        boolean.class));

                                        BranchResult hasUnsafeResult = clinitMethod.ifTrue(hasUnsafe);
                                        BytecodeCreator hasUnsafeTrueBranch = hasUnsafeResult.trueBranch();

                                        // Try block
                                        {
                                            TryBlock tryBlock = hasUnsafeTrueBranch.tryBlock();

                                            // ByteBuffer buffer = ByteBuffer.allocateDirect(1);
                                            ResultHandle buffer = tryBlock.invokeStaticMethod(
                                                    MethodDescriptor.ofMethod(ByteBuffer.class, "allocateDirect",
                                                            ByteBuffer.class,
                                                            int.class),
                                                    tryBlock.load(1));

                                            // PlatformDependent0.UNSAFE.invokeCleaner(buffer);
                                            ResultHandle unsafe = tryBlock.readStaticField(
                                                    FieldDescriptor.of("io.netty.util.internal.PlatformDependent0",
                                                            "UNSAFE",
                                                            "sun.misc.Unsafe"));
                                            tryBlock.invokeVirtualMethod(
                                                    MethodDescriptor.ofMethod("sun.misc.Unsafe",
                                                            "invokeCleaner",
                                                            void.class,
                                                            ByteBuffer.class),
                                                    unsafe,
                                                    buffer);

                                            tryBlock.assign(supportedVar, tryBlock.load(true));

                                            // Catch block
                                            CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
                                            catchBlock.assign(errorVar, catchBlock.getCaughtException());
                                        }

                                        // Handle else branch (Unsafe unavailable)
                                        BytecodeCreator hasUnsafeFalseBranch = hasUnsafeResult.falseBranch();
                                        ResultHandle unsupportedEx = hasUnsafeFalseBranch.newInstance(
                                                MethodDescriptor.ofConstructor(UnsupportedOperationException.class,
                                                        String.class),
                                                hasUnsafeFalseBranch.load("sun.misc.Unsafe unavailable"));
                                        hasUnsafeFalseBranch.assign(errorVar, unsupportedEx);

                                        // Write SUPPORTED field
                                        clinitMethod.writeStaticField(supportedFieldDescriptor, supportedVar);

                                        // Log the result

                                        // if (error == null) {
                                        //   logger.debug("java.nio.ByteBuffer.cleaner(): available");
                                        // }
                                        BranchResult errorCheck = clinitMethod.ifNull(errorVar);
                                        BytecodeCreator errorNull = errorCheck.trueBranch();
                                        errorNull.invokeInterfaceMethod(
                                                MethodDescriptor.ofMethod(
                                                        "io.netty.util.internal.logging.InternalLogger",
                                                        "debug",
                                                        void.class,
                                                        String.class),
                                                errorNull.readStaticField(loggerFieldDescriptor),
                                                errorNull.load("java.nio.ByteBuffer.cleaner(): available"));

                                        // else {
                                        //   logger.debug("java.nio.ByteBuffer.cleaner(): unavailable", error);
                                        // }
                                        BytecodeCreator errorNotNull = errorCheck.falseBranch();
                                        errorNotNull.invokeInterfaceMethod(
                                                MethodDescriptor.ofMethod(
                                                        "io.netty.util.internal.logging.InternalLogger",
                                                        "debug",
                                                        void.class,
                                                        String.class,
                                                        Throwable.class),
                                                errorNotNull.readStaticField(loggerFieldDescriptor),
                                                errorNotNull.load("java.nio.ByteBuffer.cleaner(): unavailable"),
                                                errorVar);

                                        clinitMethod.returnValue(null);
                                    }
                                }

                                {
                                    MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(className, "isSupported",
                                            boolean.class);
                                    transformer.removeMethod(methodDescriptor);
                                    MethodCreator isSupportedMethod = transformer.addMethod(methodDescriptor);
                                    isSupportedMethod.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
                                    isSupportedMethod.returnValue(isSupportedMethod.readStaticField(supportedFieldDescriptor));
                                }

                                {
                                    MethodDescriptor freeDirectBufferDescriptor = MethodDescriptor.ofMethod(className,
                                            "freeDirectBuffer", void.class, ByteBuffer.class);
                                    transformer.removeMethod(freeDirectBufferDescriptor);
                                    MethodCreator freeDirectBufferMethod = transformer.addMethod(freeDirectBufferDescriptor);
                                    freeDirectBufferMethod.setModifiers(Opcodes.ACC_PUBLIC);

                                    ResultHandle bufferParam = freeDirectBufferMethod.getMethodParam(0);

                                    // if (System.getSecurityManager() == null)
                                    ResultHandle securityManager = freeDirectBufferMethod.invokeStaticMethod(
                                            MethodDescriptor.ofMethod(System.class, "getSecurityManager",
                                                    SecurityManager.class));

                                    BranchResult securityCheck = freeDirectBufferMethod.ifNull(securityManager);

                                    // True branch: No security manager - direct call
                                    BytecodeCreator noSecurityBranch = securityCheck.trueBranch();

                                    // PlatformDependent0.UNSAFE.invokeCleaner(buffer);
                                    ResultHandle unsafe = noSecurityBranch.readStaticField(
                                            FieldDescriptor.of("io.netty.util.internal.PlatformDependent0",
                                                    "UNSAFE",
                                                    "sun.misc.Unsafe"));
                                    noSecurityBranch.invokeVirtualMethod(
                                            MethodDescriptor.ofMethod("sun.misc.Unsafe",
                                                    "invokeCleaner",
                                                    void.class,
                                                    ByteBuffer.class),
                                            unsafe,
                                            bufferParam);

                                    // False branch: With security manager - call privileged method
                                    BytecodeCreator withSecurityBranch = securityCheck.falseBranch();

                                    // freeDirectBufferPrivileged(buffer);
                                    withSecurityBranch.invokeStaticMethod(
                                            MethodDescriptor.ofMethod("io.netty.util.internal.CleanerJava9",
                                                    "freeDirectBufferPrivileged",
                                                    void.class,
                                                    ByteBuffer.class),
                                            bufferParam);

                                    // Return from method (void)
                                    freeDirectBufferMethod.returnValue(null);
                                }

                                {
                                    transformer.removeMethod("access$000", Method.class);
                                }

                                return transformer.applyTo(classVisitor);
                            }
                        })
                .build();
    }

    /**
     * When the application targets Java 21+, then we can convert {@code io.netty.util.internal.PlatformDependent0} to depend
     * explicitly on Virtual Threads instead of Netty needing to use reflection (that has a noticeable impact on startup).
     * The change makes the {@code IS_VIRTUAL_THREAD_METHOD} and {@code BASE_VIRTUAL_THREAD_CLASS} fields {@code null} while
     * also
     * converting the {@code isVirtualThread} method to:
     *
     * <pre>{@code
     * static boolean isVirtualThread() {
     *     return thread != null && thread.isVirtual();
     * }
     * }</pre>
     *
     * The reason we don't remove the aforementioned fields is that to do that we would have to transform the class loading
     * initialization method,
     * which would be to brittle.
     */
    @BuildStep
    void transformPlatformDependent0(CompiledJavaVersionBuildItem compiledJavaVersion,
            BuildProducer<BytecodeTransformerBuildItem> producer) {
        if (compiledJavaVersion.getJavaVersion().isJava21OrHigher() != CompiledJavaVersionBuildItem.JavaVersion.Status.TRUE) {
            return;
        }
        String className = "io.netty.util.internal.PlatformDependent0";
        producer.produce(new BytecodeTransformerBuildItem.Builder().setClassToTransform(className)
                .setCacheable(true).setVisitorFunction(
                        new BiFunction<>() {
                            @Override
                            public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                                ClassTransformer transformer = new ClassTransformer(className);

                                {
                                    MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(className,
                                            "getIsVirtualThreadMethod",
                                            Method.class);
                                    transformer.removeMethod(methodDescriptor);
                                    MethodCreator getIsVirtualThreadMethod = transformer.addMethod(methodDescriptor)
                                            .setModifiers(Modifier.STATIC | Modifier.PRIVATE);
                                    getIsVirtualThreadMethod.returnValue(getIsVirtualThreadMethod.loadNull());
                                }

                                {
                                    MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(className,
                                            "getBaseVirtualThreadClass",
                                            Class.class);
                                    transformer.removeMethod(methodDescriptor);
                                    MethodCreator getBaseVirtualThreadClassMethod = transformer.addMethod(methodDescriptor)
                                            .setModifiers(Modifier.STATIC | Modifier.PRIVATE);
                                    getBaseVirtualThreadClassMethod.returnValue(getBaseVirtualThreadClassMethod.loadNull());
                                }

                                {
                                    MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(className, "isVirtualThread",
                                            boolean.class, Thread.class);
                                    transformer.removeMethod(methodDescriptor);

                                    MethodCreator isVirtualThreadMethod = transformer.addMethod(methodDescriptor)
                                            .setModifiers(Modifier.STATIC);

                                    // Get the thread parameter
                                    ResultHandle threadParam = isVirtualThreadMethod.getMethodParam(0);

                                    // Check if thread is null
                                    BranchResult nullCheck = isVirtualThreadMethod.ifNull(threadParam);

                                    // If null, return false
                                    nullCheck.trueBranch().returnValue(nullCheck.trueBranch().load(false));

                                    // If not null, call thread.isVirtual()
                                    MethodDescriptor isVirtualMethod = MethodDescriptor.ofMethod(
                                            Thread.class,
                                            "isVirtual",
                                            boolean.class);
                                    ResultHandle isVirtualResult = nullCheck.falseBranch().invokeVirtualMethod(
                                            isVirtualMethod,
                                            threadParam);

                                    // Return the result
                                    nullCheck.falseBranch().returnValue(isVirtualResult);
                                }

                                return transformer.applyTo(classVisitor);
                            }
                        })
                .build());
    }
}
