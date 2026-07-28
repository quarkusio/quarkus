package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class GeneratedAnnotationTest extends AbstractGeneratedAnnotationTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(GeneratedAnnotationResource.class,
                                    GeneratedViews.class,
                                    PropertyIgnoreBean.class,
                                    NamingWithOverrideBean.class,
                                    CreatorAliasBean.class,
                                    ViewIgnoreBean.class,
                                    UnwrappedWithRenameBean.class,
                                    UnwrappedWithRenameBean.InnerAddress.class,
                                    AnySetterIgnorePropertiesBean.class,
                                    PolymorphicWithPropertyBase.class,
                                    PolymorphicWithPropertyBase.TextItem.class,
                                    PolymorphicWithPropertyBase.NumberItem.class,
                                    MultiAnnotationRecord.class,
                                    CreatorIgnoreBean.class,
                                    ValueCreatorWrapper.class,
                                    NamingAliasRecord.class,
                                    PropertyViewRecord.class,
                                    NamingViewBean.class,
                                    IgnorePropertiesCreatorRecord.class,
                                    IgnoreAnySetterBean.class,
                                    AnyGetterBean.class,
                                    FieldAnyGetterBean.class,
                                    FieldAnySetterBean.class,
                                    ManagedReferenceParent.class,
                                    ManagedReferenceChild.class,
                                    DateFormatBean.class,
                                    DurationFormatBean.class,
                                    NumberShapedTemporalBean.class,
                                    ZonedDateTimeFormatBean.class,
                                    FormatShape.class,
                                    FormatBean.class,
                                    FormatArrayShapeBean.class,
                                    FormatArrayShapeNoOrderBean.class,
                                    FormatStringShapeBean.class,
                                    FormatNumberBooleanBean.class,
                                    FormatDateTimestampBean.class,
                                    GetterSetterBean.class,
                                    IgnoredType.class,
                                    IgnoreTypeBean.class,
                                    IncludeBean.class,
                                    PropertyOrderBean.class,
                                    RawValueBean.class,
                                    PackageProtectedBean.class,
                                    SpecialCharPropertyBean.class,
                                    TestWithJsonPropertyDto.class,
                                    UnwrappedIgnorePropertiesBean.class,
                                    UnwrappedIgnorePropertiesBean.User.class,
                                    UnwrappedWithPrefixBean.class,
                                    UnwrappedWithPrefixBean.Address.class)
                            .addAsResource(new StringAsset(
                                    "quarkus.jackson.fail-on-unknown-properties=true\n" +
                                            "quarkus.jackson.default-view-inclusion=true\n" +
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=false\n"),
                                    "application.properties");
                }
            });
}
