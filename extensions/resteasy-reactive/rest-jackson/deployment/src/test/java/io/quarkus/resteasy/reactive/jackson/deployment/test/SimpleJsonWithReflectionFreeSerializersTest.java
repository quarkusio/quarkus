package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

public class SimpleJsonWithReflectionFreeSerializersTest extends SimpleJsonTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
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
                                    NestedInterface.class, StateRecord.class, MapWrapper.class)
                            .addAsResource(new StringAsset("admin-expression=admin\n" +
                                    "user-expression=user\n" +
                                    "birth-date-roles=alice,bob\n" +
                                    "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });
}
