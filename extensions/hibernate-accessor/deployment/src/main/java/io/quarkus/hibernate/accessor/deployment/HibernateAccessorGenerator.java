package io.quarkus.hibernate.accessor.deployment;

import java.lang.constant.ClassDesc;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.asm.aot.AsmAotGenerator;
import org.hibernate.accessor.asm.aot.GenerationNames;

import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.hibernate.accessor.runtime.AccessorFactoryBootstrap;

/**
 * Configures the Hibernate Accessor AOT generator with Quarkus-specific class names.
 * See {@link HibernateAccessorProcessor} for an overview of how the generated components work together.
 */
final class HibernateAccessorGenerator {

    public static final String GENERATED_INSTANTIATOR_IMPL = "io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorInstantiatorImpl";

    public static final String GENERATED_WRITER_IMPL = "io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorValueWriterImpl";
    public static final String GENERATED_READER_IMPL = "io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorValueReaderImpl";
    public static final String QUARKUS_HIBERNATE_ACCESSOR_FACTORY = "io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorFactory";

    static final String GENERATED_BOOTSTRAP = "io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorFactoryBootstrap";

    // Both the generator and its names are immutable, and the generator is used once per transformed
    // class, so a single shared instance is enough.
    private static final AsmAotGenerator GENERATOR = new AsmAotGenerator(new GenerationNames(
            QUARKUS_HIBERNATE_ACCESSOR_FACTORY,
            GENERATED_READER_IMPL,
            GENERATED_WRITER_IMPL,
            GENERATED_INSTANTIATOR_IMPL));

    private HibernateAccessorGenerator() {
    }

    static void generateBootstrap(BuildProducer<GeneratedClassBuildItem> generatedClasses, boolean withFallback) {
        Gizmo.create(new GeneratedClassGizmo2Adaptor(generatedClasses, null, null, null, true))
                .class_(GENERATED_BOOTSTRAP, cc -> {
                    cc.public_();
                    cc.implements_(AccessorFactoryBootstrap.class);
                    cc.defaultConstructor();
                    cc.method("create", mc -> {
                        mc.public_();
                        mc.returning(AccessorFactory.class);
                        var fallback = mc.parameter("fallback", AccessorFactory.class);
                        mc.body(bc -> {
                            ClassDesc factory = ClassDesc.of(QUARKUS_HIBERNATE_ACCESSOR_FACTORY);
                            bc.return_(withFallback
                                    ? bc.invokeStatic(ClassMethodDesc.of(factory, "create", AccessorFactory.class,
                                            AccessorFactory.class), fallback)
                                    : bc.invokeStatic(ClassMethodDesc.of(factory, "create", AccessorFactory.class)));
                        });
                    });
                });
    }

    static AsmAotGenerator generator() {
        return GENERATOR;
    }

}
