package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.logging.Level;

import org.apache.pulsar.client.impl.conf.ClientConfigurationData;
import org.apache.pulsar.client.impl.conf.ConsumerConfigurationData;
import org.apache.pulsar.client.impl.conf.ProducerConfigurationData;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.pulsar.PulsarRuntimeConfigProducer;

public class SmallRyeReactiveMessagingPulsarProcessor {

    static Logger log = Logger.getLogger(SmallRyeReactiveMessagingPulsarProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_REACTIVE_MESSAGING_PULSAR);
    }

    @BuildStep
    public AdditionalBeanBuildItem runtimeConfig() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(PulsarRuntimeConfigProducer.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    void logging(BuildProducer<LogCategoryBuildItem> log) {
        log.produce(new LogCategoryBuildItem("org.apache.pulsar.common.util.netty.DnsResolverUtil", Level.OFF));
    }

    @BuildStep
    void disableStatsLogging(BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfig) {
        runtimeConfig.produce(new RunTimeConfigurationDefaultBuildItem(
                "mp.messaging.connector.smallrye-pulsar.statsIntervalSeconds", "0"));
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResources() {
        return new NativeImageResourceBuildItem(
                "org/asynchttpclient/config/ahc-default.properties",
                "org/asynchttpclient/config/ahc.properties");
    }

    @BuildStep
    void bytecodeTransformer(BuildProducer<BytecodeTransformerBuildItem> producer) {
        String klass = "org.asynchttpclient.request.body.multipart.FileLikePart";

        producer.produce(new BytecodeTransformerBuildItem(klass, new BiFunction<String, ClassVisitor, ClassVisitor>() {
            @Override
            public ClassVisitor apply(String cls, ClassVisitor classVisitor) {
                return new FileLikePartJavaxRemover(Gizmo.ASM_API_VERSION, classVisitor);
            }

        }));
    }

    private class FileLikePartJavaxRemover extends ClassVisitor {
        public FileLikePartJavaxRemover(int version, ClassVisitor cv) {
            super(version, cv);
            log.debug("Removing javax.activation from FileLikePart");
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (name.equals("MIME_TYPES_FILE_TYPE_MAP")) {
                return null;
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        // invoked for every method
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
            if (visitor == null) {
                return null;
            }
            if (name.equals("<clinit>")) {
                return new MethodVisitor(Gizmo.ASM_API_VERSION, visitor) {
                    @Override
                    public void visitCode() {
                        mv.visitCode();
                        mv.visitInsn(Opcodes.RETURN);// our new code
                    }
                };
            }
            if (name.equals("computeContentType")) {
                return new MethodVisitor(Gizmo.ASM_API_VERSION, visitor) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        visitVarInsn(Opcodes.ALOAD, 1); // first param
                        visitInsn(Opcodes.ARETURN);
                    }
                };
            }
            visitor.visitMaxs(0, 0);
            return visitor;
        }

    }

    @BuildStep
    IndexDependencyBuildItem indexPulsar() {
        return new IndexDependencyBuildItem("org.apache.pulsar", "pulsar-client-original");
    }

    @BuildStep
    public NativeImageConfigBuildItem pulsarRuntimeInitialized(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExtensionSslNativeSupportBuildItem> nativeSslSupport) {
        nativeSslSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.SMALLRYE_REACTIVE_MESSAGING_PULSAR));
        reflectiveClass.produce(ReflectiveClassBuildItem
                .builder(ClientConfigurationData.class.getName(),
                        ProducerConfigurationData.class.getName(),
                        ConsumerConfigurationData.class.getName(),
                        "org.apache.pulsar.client.impl.auth.oauth2.KeyFile",
                        "org.apache.pulsar.client.impl.auth.oauth2.protocol.Metadata",
                        "org.apache.pulsar.client.impl.auth.oauth2.protocol.TokenResult",
                        "org.apache.pulsar.client.impl.auth.oauth2.protocol.TokenError",
                        "org.apache.pulsar.client.impl.auth.oauth2.protocol.ClientCredentialsExchangeRequest",
                        "org.apache.pulsar.client.api.url.DataURLStreamHandler",
                        "com.google.protobuf.GeneratedMessageV3",
                        "org.apache.pulsar.common.protocol.schema.ProtobufNativeSchemaData",
                        "org.apache.pulsar.client.impl.schema.ProtobufNativeSchema$ProtoBufParsingInfo",
                        "org.apache.pulsar.client.impl.schema.ProtobufSchema$ProtoBufParsingInfo",
                        "org.apache.pulsar.common.schema.KeyValue")
                .fields(true)
                .methods(true)
                .constructors(true)
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem
                .builder("org.apache.pulsar.client.util.SecretsSerializer")
                .constructors().build());

        Collection<ClassInfo> authPluginClasses = combinedIndex.getIndex()
                .getAllKnownImplementors(DotNames.PULSAR_AUTHENTICATION);
        for (ClassInfo authPluginClass : authPluginClasses) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(authPluginClass.name().toString())
                    .constructors().build());
        }

        return NativeImageConfigBuildItem.builder()
                .addNativeImageSystemProperty("io.netty.handler.ssl.noOpenSsl", "true")
                .addRuntimeInitializedClass("org.apache.pulsar.common.allocator.PulsarByteBufAllocator")
                .addRuntimeInitializedClass("org.apache.pulsar.common.protocol.Commands")
                .addRuntimeInitializedClass("org.apache.pulsar.client.impl.auth.oauth2.protocol.TokenClient")
                .addRuntimeInitializedClass("org.apache.pulsar.client.impl.crypto.MessageCryptoBc")
                .addRuntimeInitializedClass("org.apache.pulsar.client.impl.schema.generic.GenericProtobufNativeSchema")
                .addRuntimeInitializedClass("org.apache.pulsar.client.impl.Backoff")
                .addRuntimeInitializedClass("org.apache.pulsar.client.impl.ConnectionPool")
                .addRuntimeInitializedClass("org.apache.pulsar.client.impl.ControlledClusterFailover")
                .addRuntimeInitializedClass("org.apache.pulsar.client.impl.HttpClient")
                .addRuntimeInitializedClass("org.apache.pulsar.client.util.WithSNISslEngineFactory")
                .addRuntimeInitializedClass("com.yahoo.sketches.quantiles.DoublesSketch")
                .addRuntimeInitializedClass("io.netty.buffer.PooledByteBufAllocator")
                .addRuntimeInitializedClass("io.netty.buffer.UnpooledByteBufAllocator$InstrumentedUnpooledHeapByteBuf")
                .addRuntimeInitializedClass("io.netty.incubator.channel.uring.IOUringEventLoopGroup")
                .addRuntimeInitializedClass("io.netty.incubator.channel.uring.Native")
                .addRuntimeInitializedClass("io.netty.incubator.channel.uring.IOUring")
                .addRuntimeInitializedClass("org.asynchttpclient.RequestBuilderBase")
                .addRuntimeInitializedClass("org.asynchttpclient.RequestBuilder")
                .addRuntimeInitializedClass("org.asynchttpclient.BoundRequestBuilder")
                .addRuntimeInitializedClass("org.asynchttpclient.ntlm.NtlmEngine")
                .addRuntimeInitializedClass("sun.awt.dnd.SunDropTargetContextPeer$EventDispatcher")
                .build();
    }

}
