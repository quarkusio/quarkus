package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class UnsupportedAnnotationTest extends AbstractUnsupportedAnnotationTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(UnsupportedAnnotationResource.class,
                                    AnyGetterBean.class,
                                    AutoDetectBean.class,
                                    ManagedReferenceParent.class,
                                    ManagedReferenceChild.class,
                                    FormatShape.class,
                                    FormatBean.class,
                                    GetterSetterBean.class,
                                    IgnoredType.class,
                                    IgnoreTypeBean.class,
                                    IncludeBean.class,
                                    PropertyOrderBean.class,
                                    RawValueBean.class)
                            .addAsResource(new StringAsset(
                                    "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=false\n"),
                                    "application.properties");
                }
            });
}
