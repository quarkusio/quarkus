package io.quarkus.hibernate.orm.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

class AdditionalPersistenceUnitExternalEntityTest {

    static final String PERSISTENCE_UNIT_NAME = "external";
    static final String EXTERNAL_ENTITY_CLASS_NAME = "com.example.external.ExternalEntity";
    private static final String EXTERNAL_INTERNAL_NAME = "com/example/external/ExternalEntity";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .add(new ByteArrayAsset(generateEntityBytecode()), EXTERNAL_INTERNAL_NAME + ".class"))
            .addBuildChainCustomizer(buildCustomizer())
            .withConfiguration("""
                    quarkus.datasource.vector.db-kind=h2
                    quarkus.hibernate-orm."external".schema-management.strategy=drop-and-create
                    """);

    @Inject
    @PersistenceUnit(PERSISTENCE_UNIT_NAME)
    Session externalSession;

    @Test
    @Transactional
    void externalEntityIsUsable() {
        Object entity;
        try {
            Class<?> entityClass = Thread.currentThread().getContextClassLoader()
                    .loadClass(EXTERNAL_ENTITY_CLASS_NAME);
            entity = entityClass.getConstructor(String.class).newInstance("hello");
            externalSession.persist(entity);
            externalSession.flush();
            externalSession.clear();

            Object loaded = externalSession.find(entityClass, entityClass.getMethod("getId").invoke(entity));
            assertThat(loaded).isNotNull();
            assertThat(entityClass.getMethod("getName").invoke(loaded)).isEqualTo("hello");
        } catch (Exception e) {
            throw new RuntimeException("Failed to use external entity", e);
        }
    }

    private static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(AdditionalPersistenceUnitBuildItem.builder(PERSISTENCE_UNIT_NAME)
                                .dataSourceName("vector")
                                .managedClass(EXTERNAL_ENTITY_CLASS_NAME)
                                .build());
                    }
                })
                        .produces(AdditionalPersistenceUnitBuildItem.class)
                        .build();
            }
        };
    }

    private static byte[] generateEntityBytecode() {
        byte[][] result = new byte[1][];
        Gizmo g = Gizmo.create((path, bytes) -> {
            if (path.endsWith(".class")) {
                result[0] = bytes;
            }
        });
        g.class_(EXTERNAL_ENTITY_CLASS_NAME, cc -> {
            cc.public_();
            cc.addAnnotation(Entity.class);

            FieldDesc idField = cc.field("id", fc -> {
                fc.setType(Long.class);
                fc.private_();
                fc.addAnnotation(Id.class);
                fc.addAnnotation(GeneratedValue.class, ac -> {
                    ac.add(GeneratedValue::strategy, GenerationType.IDENTITY);
                });
            });

            FieldDesc nameField = cc.field("name", fc -> {
                fc.setType(String.class);
                fc.private_();
            });

            ConstructorDesc superCtor = ConstructorDesc.of(cc.superClass());

            cc.defaultConstructor();

            cc.constructor(ctor -> {
                ctor.public_();
                ParamVar nameParam = ctor.parameter("name", String.class);
                ctor.body(bc -> {
                    bc.invokeSpecial(superCtor, ctor.this_());
                    bc.set(ctor.this_().field(nameField), nameParam);
                    bc.return_();
                });
            });

            cc.method("getId", m -> {
                m.public_();
                m.returning(Long.class);
                m.body(bc -> bc.return_(m.this_().field(idField)));
            });

            cc.method("getName", m -> {
                m.public_();
                m.returning(String.class);
                m.body(bc -> bc.return_(m.this_().field(nameField)));
            });

            cc.method("setName", m -> {
                m.public_();
                ParamVar nameParam = m.parameter("name", String.class);
                m.body(bc -> {
                    bc.set(m.this_().field(nameField), nameParam);
                    bc.return_();
                });
            });
        });
        return result[0];
    }
}
