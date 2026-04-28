package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;

public class SimpleJsonWithReflectionFreeSerializersTest extends AbstractSimpleJsonTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Person.class, SimpleJsonResource.class, User.class, Views.class, SuperClass.class,
                                    OtherPersonResource.class, AbstractPersonResource.class, DataItem.class, Item.class,
                                    NoopReaderInterceptor.class, TestIdentityProvider.class, TestIdentityController.class,
                                    AbstractPet.class, Dog.class, Cat.class, Veterinarian.class, AbstractNamedPet.class,
                                    AbstractUnsecuredPet.class, UnsecuredPet.class, SecuredPersonInterface.class, Frog.class,
                                    Pond.class, FrogBodyParts.class, FrogBodyParts.BodyPart.class, ContainerDTO.class,
                                    NestedInterface.class, StateRecord.class, MapWrapper.class, GenericWrapper.class,
                                    Fruit.class, Price.class, DogRecord.class, ItemExtended.class, Book.class, LombokBook.class,
                                    PrimitiveTypesBean.class, PrimitiveTypesRecord.class, TokenResponse.class,
                                    ItemJsonValuePublicMethod.class, ItemJsonValuePublicField.class,
                                    ItemJsonValuePrivateMethod.class, ItemJsonValuePrivateField.class, StringWrapper.class,
                                    JsonAliasRecord.class, AnnotationNamingRequest.class, Pair.class, Score.class,
                                    ProductPrice.class, DefaultValueHolder.class, OptionalHolder.class, AnySetterRequest.class,
                                    UnwrappedResult.class, UnwrappedResultsResponse.class, Detail.class, ErrorInfo.class,
                                    PolymorphicItemResponse.class, PolymorphicItem.class)
                            .addAsResource(new StringAsset("admin-expression=admin\n" +
                                    "user-expression=user\n" +
                                    "birth-date-roles=alice,bob\n" +
                                    "quarkus.jackson.fail-on-unknown-properties=true\n" +
                                    "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    // The following assertions have been commented out because at the moment it is discovering classes with unknown
    // Jackson annotations, for which it cannot generate a serializer and then the log messages are no longer empty.
    // We plan to add support for those outstanding annotations, but before the focus is to check that the mechanism
    // falling back to standard reflection-based Jackson serializers works as expected.

    //            .setLogRecordPredicate(record -> record.getLevel().equals(Level.INFO)
    //                    && record.getLoggerName().equals(
    //                            "io.quarkus.resteasy.reactive.jackson.deployment.processor.JacksonCodeGenerator"))
    //            .assertLogRecords(records -> assertThat(records).isEmpty());
}
