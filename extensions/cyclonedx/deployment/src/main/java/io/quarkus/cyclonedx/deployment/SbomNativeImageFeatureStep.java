package io.quarkus.cyclonedx.deployment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.constant.ClassDesc;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

import org.graalvm.nativeimage.hosted.Feature;

import io.quarkus.cyclonedx.deployment.spi.EmbeddedSbomMetadataBuildItem;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 * Generates a GraalVM {@link Feature} that embeds the application SBOM
 * into the native image as {@code sbom} and {@code sbom_length} global symbols,
 * following the <a href="https://www.graalvm.org/jdk25/security-guide/native-image/sbom/">GraalVM SBOM spec</a>.
 * <p>
 * Internal GraalVM APIs ({@code CGlobalDataFactory}, {@code CGlobalDataFeature}, {@code Word})
 * are referenced via {@link ClassMethodDesc} to avoid compile-time dependencies.
 */
public class SbomNativeImageFeatureStep {

    static final String SBOM_EMBED_FEATURE = "io.quarkus.runner.SbomEmbedFeature";

    private static final MethodDesc GET_RESOURCE_AS_STREAM = MethodDesc.of(Class.class,
            "getResourceAsStream", InputStream.class, String.class);
    private static final MethodDesc READ_ALL_BYTES = MethodDesc.of(InputStream.class,
            "readAllBytes", byte[].class);
    private static final MethodDesc CLOSE_INPUT_STREAM = MethodDesc.of(InputStream.class,
            "close", void.class);
    private static final MethodDesc GZIP_WRITE = MethodDesc.of(GZIPOutputStream.class,
            "write", void.class, byte[].class);
    private static final MethodDesc GZIP_CLOSE = MethodDesc.of(GZIPOutputStream.class,
            "close", void.class);
    private static final MethodDesc BAOS_TO_BYTE_ARRAY = MethodDesc.of(ByteArrayOutputStream.class,
            "toByteArray", byte[].class);
    private static final ConstructorDesc GZIP_OUTPUT_STREAM_CTOR = ConstructorDesc.of(
            GZIPOutputStream.class, OutputStream.class);

    private static final ClassDesc CD_CGLOBAL_DATA = ClassDesc.of("com.oracle.svm.core.c.CGlobalData");
    private static final ClassDesc CD_CGLOBAL_DATA_FACTORY = ClassDesc.of("com.oracle.svm.core.c.CGlobalDataFactory");
    private static final ClassDesc CD_CGLOBAL_DATA_FEATURE = ClassDesc.of("com.oracle.svm.hosted.c.CGlobalDataFeature");
    private static final ClassDesc CD_WORD = ClassDesc.of("jdk.graal.compiler.word.Word");
    private static final ClassDesc CD_UNSIGNED_WORD = ClassDesc.of("org.graalvm.word.UnsignedWord");
    private static final ClassDesc CD_WORD_BASE = ClassDesc.of("org.graalvm.word.WordBase");

    private static final ClassMethodDesc CREATE_BYTES = ClassMethodDesc.of(
            CD_CGLOBAL_DATA_FACTORY, "createBytes", CD_CGLOBAL_DATA,
            ClassDesc.of("java.util.function.Supplier"), ClassDesc.of("java.lang.String"));
    private static final ClassMethodDesc CREATE_WORD = ClassMethodDesc.of(
            CD_CGLOBAL_DATA_FACTORY, "createWord", CD_CGLOBAL_DATA,
            CD_WORD_BASE, ClassDesc.of("java.lang.String"));
    private static final ClassMethodDesc CGLOBAL_DATA_FEATURE_SINGLETON = ClassMethodDesc.of(
            CD_CGLOBAL_DATA_FEATURE, "singleton", CD_CGLOBAL_DATA_FEATURE);
    private static final ClassMethodDesc REGISTER_WITH_GLOBAL_SYMBOL = ClassMethodDesc.of(
            CD_CGLOBAL_DATA_FEATURE, "registerWithGlobalSymbol", ClassDesc.ofDescriptor("V"), CD_CGLOBAL_DATA);
    private static final ClassMethodDesc WORD_UNSIGNED = ClassMethodDesc.of(
            CD_WORD, "unsigned", CD_UNSIGNED_WORD, ClassDesc.ofDescriptor("J"));

    @BuildStep
    void generateSbomEmbedFeature(
            Optional<EmbeddedSbomMetadataBuildItem> embeddedSbomMetadata,
            BuildProducer<GeneratedNativeImageClassBuildItem> nativeImageClass,
            BuildProducer<NativeImageFeatureBuildItem> features,
            BuildProducer<JPMSExportBuildItem> jpmsExports) {
        if (embeddedSbomMetadata.isEmpty()) {
            return;
        }

        EmbeddedSbomMetadataBuildItem metadata = embeddedSbomMetadata.get();
        String resourceName = metadata.getResourceName();
        boolean isCompressed = metadata.isCompressed();

        jpmsExports.produce(new JPMSExportBuildItem("org.graalvm.nativeimage.builder", "com.oracle.svm.core.c"));
        jpmsExports.produce(new JPMSExportBuildItem("org.graalvm.nativeimage.builder", "com.oracle.svm.hosted.c"));
        jpmsExports.produce(new JPMSExportBuildItem("jdk.graal.compiler", "jdk.graal.compiler.word"));

        Gizmo g = Gizmo.create(new GeneratedClassGizmo2Adaptor(
                item -> nativeImageClass
                        .produce(new GeneratedNativeImageClassBuildItem(item.binaryName(), item.getClassData())),
                item -> {
                },
                false));

        g.class_(SBOM_EMBED_FEATURE, cc -> {
            cc.implements_(Feature.class);
            cc.defaultConstructor();

            cc.method("getDescription", mc -> {
                mc.returning(String.class);
                mc.body(b -> b.return_(Const.of("Embeds the application SBOM in the native image")));
            });

            cc.method("afterAnalysis", mc -> {
                mc.parameter("access", Feature.AfterAnalysisAccess.class);
                mc.body(b0 -> {
                    b0.try_(t -> {
                        t.body(tb -> {
                            Expr clazz = Const.of(cc.type());
                            LocalVar is = tb.localVar("is",
                                    tb.invokeVirtual(GET_RESOURCE_AS_STREAM, clazz,
                                            Const.of("/" + resourceName)));
                            tb.ifNull(is, nb -> {
                                nb.throw_(RuntimeException.class,
                                        "SBOM resource not found on classpath: " + resourceName);
                            });
                            LocalVar resourceBytes = tb.localVar("resourceBytes",
                                    tb.invokeVirtual(READ_ALL_BYTES, is));
                            tb.invokeVirtual(CLOSE_INPUT_STREAM, is);

                            LocalVar sbomBytes;
                            if (isCompressed) {
                                sbomBytes = tb.localVar("sbomBytes", resourceBytes);
                            } else {
                                LocalVar bout = tb.localVar("bout", tb.new_(ByteArrayOutputStream.class));
                                LocalVar gout = tb.localVar("gout", tb.new_(GZIP_OUTPUT_STREAM_CTOR, bout));
                                tb.invokeVirtual(GZIP_WRITE, gout, resourceBytes);
                                tb.invokeVirtual(GZIP_CLOSE, gout);
                                sbomBytes = tb.localVar("sbomBytes", tb.invokeVirtual(BAOS_TO_BYTE_ARRAY, bout));
                            }

                            Expr supplier = tb.lambda(Supplier.class, lc -> {
                                var capturedBytes = lc.capture(sbomBytes);
                                lc.body(lb -> lb.return_(capturedBytes));
                            });

                            LocalVar sbomData = tb.localVar("sbomData",
                                    tb.invokeStatic(CREATE_BYTES, supplier, Const.of("sbom")));
                            LocalVar cgFeature = tb.localVar("cgFeature",
                                    tb.invokeStatic(CGLOBAL_DATA_FEATURE_SINGLETON));
                            tb.invokeVirtual(REGISTER_WITH_GLOBAL_SYMBOL, cgFeature, sbomData);

                            Expr unsignedLen = tb.invokeStatic(WORD_UNSIGNED,
                                    tb.cast(sbomBytes.length(), long.class));
                            LocalVar sbomLenData = tb.localVar("sbomLenData",
                                    tb.invokeStatic(CREATE_WORD, unsignedLen, Const.of("sbom_length")));
                            tb.invokeVirtual(REGISTER_WITH_GLOBAL_SYMBOL, cgFeature, sbomLenData);
                        });
                        t.catch_(Exception.class, "e", (cb, e) -> {
                            cb.throw_(RuntimeException.class, e);
                        });
                    });
                    b0.return_();
                });
            });
        });

        features.produce(new NativeImageFeatureBuildItem(SBOM_EMBED_FEATURE));
    }
}