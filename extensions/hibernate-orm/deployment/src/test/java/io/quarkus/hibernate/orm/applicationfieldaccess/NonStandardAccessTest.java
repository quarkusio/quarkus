package io.quarkus.hibernate.orm.applicationfieldaccess;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that non-standard access to fields by the application works correctly, i.e. when exposing fields through
 * accessors with non-standard names, static accessors, accessors defined in a subclass, or accessors defined in an
 * inner class.
 */
public class NonStandardAccessTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAbstractMappedSuperClass.class, MyAbstractEntity.class,
                            MyAbstractConfusingEntity.class, MyConcreteEntity.class)
                    .addClass(ExternalClassAccessors.class).addClass(AccessDelegate.class))
            .withConfigurationResource("application.properties");

    @Inject
    EntityManager em;

    @Test
    public void nonStandardInstanceGetterSetterPublicField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.nonStandardSetterForPublicField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.nonStandardGetterForPublicField();
            }
        });
    }

    @Test
    public void nonStandardInstanceGetterSetterProtectedField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.nonStandardSetterForProtectedField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.nonStandardGetterForProtectedField();
            }
        });
    }

    @Test
    public void nonStandardInstanceGetterSetterPackagePrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.nonStandardSetterForPackagePrivateField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.nonStandardGetterForPackagePrivateField();
            }
        });
    }

    @Test
    public void nonStandardInstanceGetterSetterPrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.nonStandardSetterForPrivateField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.nonStandardGetterForPrivateField();
            }
        });
    }

    @Test
    public void staticGetterSetterPublicField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                MyConcreteEntity.staticSetPublicField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return MyConcreteEntity.staticGetPublicField(entity);
            }
        });
    }

    @Test
    public void staticGetterSetterProtectedField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                MyConcreteEntity.staticSetProtectedField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return MyConcreteEntity.staticGetProtectedField(entity);
            }
        });
    }

    @Test
    public void staticGetterSetterPackagePrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                MyConcreteEntity.staticSetPackagePrivateField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return MyConcreteEntity.staticGetPackagePrivateField(entity);
            }
        });
    }

    @Test
    public void staticGetterSetterPrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                MyConcreteEntity.staticSetPrivateField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return MyConcreteEntity.staticGetPrivateField(entity);
            }
        });
    }

    @Test
    public void innerClassStaticGetterSetterPublicField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                MyConcreteEntity.InnerClassAccessors.staticSetPublicField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return MyConcreteEntity.InnerClassAccessors.staticGetPublicField(entity);
            }
        });
    }

    @Test
    public void innerClassStaticGetterSetterProtectedField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                MyConcreteEntity.InnerClassAccessors.staticSetProtectedField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return MyConcreteEntity.InnerClassAccessors.staticGetProtectedField(entity);
            }
        });
    }

    @Test
    public void innerClassStaticGetterSetterPackagePrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                MyConcreteEntity.InnerClassAccessors.staticSetPackagePrivateField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return MyConcreteEntity.InnerClassAccessors.staticGetPackagePrivateField(entity);
            }
        });
    }

    @Test
    public void innerClassStaticGetterSetterPrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                MyConcreteEntity.InnerClassAccessors.staticSetPrivateField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return MyConcreteEntity.InnerClassAccessors.staticGetPrivateField(entity);
            }
        });
    }

    @Test
    public void innerClassInstanceGetterSetterPublicField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                new MyConcreteEntity.InnerClassAccessors().instanceSetPublicField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return new MyConcreteEntity.InnerClassAccessors().instanceGetPublicField(entity);
            }
        });
    }

    @Test
    public void innerClassInstanceGetterSetterProtectedField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                new MyConcreteEntity.InnerClassAccessors().instanceSetProtectedField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return new MyConcreteEntity.InnerClassAccessors().instanceGetProtectedField(entity);
            }
        });
    }

    @Test
    public void innerClassInstanceGetterSetterPackagePrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                new MyConcreteEntity.InnerClassAccessors().instanceSetPackagePrivateField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return new MyConcreteEntity.InnerClassAccessors().instanceGetPackagePrivateField(entity);
            }
        });
    }

    @Test
    public void innerClassInstanceGetterSetterPrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                new MyConcreteEntity.InnerClassAccessors().instanceSetPrivateField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return new MyConcreteEntity.InnerClassAccessors().instanceGetPrivateField(entity);
            }
        });
    }

    @Test
    public void externalClassStaticGetterSetterPublicField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                ExternalClassAccessors.staticSetPublicField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return ExternalClassAccessors.staticGetPublicField(entity);
            }
        });
    }

    @Test
    public void externalClassStaticGetterSetterProtectedField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                ExternalClassAccessors.staticSetProtectedField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return ExternalClassAccessors.staticGetProtectedField(entity);
            }
        });
    }

    @Test
    public void externalClassStaticGetterSetterPackagePrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                ExternalClassAccessors.staticSetPackagePrivateField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return ExternalClassAccessors.staticGetPackagePrivateField(entity);
            }
        });
    }

    @Test
    public void externalClassInstanceGetterSetterPublicField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                new ExternalClassAccessors().instanceSetPublicField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return new ExternalClassAccessors().instanceGetPublicField(entity);
            }
        });
    }

    @Test
    public void externalClassInstanceGetterSetterProtectedField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                new ExternalClassAccessors().instanceSetProtectedField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return new ExternalClassAccessors().instanceGetProtectedField(entity);
            }
        });
    }

    @Test
    public void externalClassInstanceGetterSetterPackagePrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                new ExternalClassAccessors().instanceSetPackagePrivateField(entity, value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return new ExternalClassAccessors().instanceGetPackagePrivateField(entity);
            }
        });
    }

    @Test
    public void mappedSuperClassSubClassInstanceGetterSetterPublicField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.setAbstractEntityPublicField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.getAbstractEntityPublicField();
            }
        });
    }

    @Test
    public void mappedSuperClassSubClassInstanceGetterSetterProtectedField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.setAbstractEntityProtectedField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.getAbstractEntityProtectedField();
            }
        });
    }

    @Test
    public void mappedSuperClassSubClassInstanceGetterSetterPackagePrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.setAbstractEntityPackagePrivateField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.getAbstractEntityPackagePrivateField();
            }
        });
    }

    @Test
    public void mappedSuperClassSubClassNonStandardInstanceGetterSetterPublicField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.nonStandardSetterForAbstractEntityPublicField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.nonStandardGetterForAbstractEntityPublicField();
            }
        });
    }

    @Test
    public void mappedSuperClassSubClassNonStandardInstanceGetterSetterProtectedField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.nonStandardSetterForAbstractEntityProtectedField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.nonStandardGetterForAbstractEntityProtectedField();
            }
        });
    }

    @Test
    public void mappedSuperClassSubClassNonStandardInstanceGetterSetterPackagePrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.nonStandardSetterForAbstractEntityPackagePrivateField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.nonStandardGetterForAbstractEntityPackagePrivateField();
            }
        });
    }

    @Test
    public void entitySubClassInstanceGetterSetterPublicField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.setAbstractEntityPublicField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.getAbstractEntityPublicField();
            }
        });
    }

    @Test
    public void entitySubClassInstanceGetterSetterProtectedField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.setAbstractEntityProtectedField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.getAbstractEntityProtectedField();
            }
        });
    }

    @Test
    public void entitySubClassInstanceGetterSetterPackagePrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.setAbstractEntityPackagePrivateField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.getAbstractEntityPackagePrivateField();
            }
        });
    }

    @Test
    public void entitySubClassNonStandardInstanceGetterSetterPublicField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.nonStandardSetterForAbstractEntityPublicField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.nonStandardGetterForAbstractEntityPublicField();
            }
        });
    }

    @Test
    public void entitySubClassNonStandardInstanceGetterSetterProtectedField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.nonStandardSetterForAbstractEntityProtectedField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.nonStandardGetterForAbstractEntityProtectedField();
            }
        });
    }

    @Test
    public void entitySubClassNonStandardInstanceGetterSetterPackagePrivateField() {
        doTestFieldAccess(new AccessDelegate() {
            @Override
            public void setValue(MyConcreteEntity entity, Long value) {
                entity.nonStandardSetterForAbstractEntityPackagePrivateField(value);
            }

            @Override
            public Long getValue(MyConcreteEntity entity) {
                return entity.nonStandardGetterForAbstractEntityPackagePrivateField();
            }
        });
    }

    // Ideally we'd make this a @ParameterizedTest and pass the access delegate as parameter,
    // but we cannot do that due to JUnit using a different classloader than the test.
    private void doTestFieldAccess(AccessDelegate delegate) {
        Long id = QuarkusTransaction.disallowingExisting().call(() -> {
            var entity = new MyConcreteEntity();
            em.persist(entity);
            return entity.id;
        });

        QuarkusTransaction.disallowingExisting().run(() -> {
            var entity = em.find(MyConcreteEntity.class, id);
            assertThat(delegate.getValue(entity)).as("Loaded value before update").isNull();
        });

        QuarkusTransaction.disallowingExisting().run(() -> {
            var entity = em.getReference(MyConcreteEntity.class, id);
            // Since field access is replaced with accessor calls,
            // we expect this change to be detected by dirty tracking and persisted.
            delegate.setValue(entity, 42L);
        });

        QuarkusTransaction.disallowingExisting().run(() -> {
            var entity = em.find(MyConcreteEntity.class, id);
            // We're working on an initialized entity.
            assertThat(entity).as("find() should return uninitialized entity").returns(true, Hibernate::isInitialized);
            // The above should have persisted a value that passes the assertion.
            assertThat(delegate.getValue(entity)).as("Loaded value after update").isEqualTo(42L);
        });

        QuarkusTransaction.disallowingExisting().run(() -> {
            var entity = em.getReference(MyConcreteEntity.class, id);
            // We're working on an uninitialized entity.
            assertThat(entity).as("getReference() should return uninitialized entity").returns(false,
                    Hibernate::isInitialized);
            // The above should have persisted a value that passes the assertion.
            assertThat(delegate.getValue(entity)).as("Lazily loaded value after update").isEqualTo(42L);
            // Accessing the value should trigger initialization of the entity.
            assertThat(entity).as("Getting the value should initialize the entity").returns(true,
                    Hibernate::isInitialized);
        });
    }

    @MappedSuperclass
    public static abstract class MyAbstractMappedSuperClass {
        public Long abstractMappedSuperClassPublicField;

        protected Long abstractMappedSuperClassProtectedField;

        Long abstractMappedSuperClassPackagePrivateField;
    }

    @Entity(name = "abstract")
    public static abstract class MyAbstractEntity extends MyAbstractMappedSuperClass {
        @Id
        @GeneratedValue
        public long id;

        public Long abstractEntityPublicField;

        protected Long abstractEntityProtectedField;

        Long abstractEntityPackagePrivateField;
    }

    // Just to confuse panache: the fields are not declared in the *direct* superclass.
    @Entity(name = "abstract2")
    public static abstract class MyAbstractConfusingEntity extends MyAbstractEntity {
    }

    @Entity(name = "concrete")
    public static class MyConcreteEntity extends MyAbstractConfusingEntity {
        public Long publicField;
        protected Long protectedField;
        Long packagePrivateField;
        private Long privateField;

        public Long getAbstractMappedSuperClassPublicField() {
            return abstractMappedSuperClassPublicField;
        }

        public void setAbstractMappedSuperClassPublicField(Long abstractMappedSuperClassPublicField) {
            this.abstractMappedSuperClassPublicField = abstractMappedSuperClassPublicField;
        }

        public Long nonStandardGetterForAbstractMappedSuperClassPublicField() {
            return abstractMappedSuperClassPublicField;
        }

        public void nonStandardSetterForAbstractMappedSuperClassPublicField(Long value) {
            abstractMappedSuperClassPublicField = value;
        }

        public Long getAbstractMappedSuperClassProtectedField() {
            return abstractMappedSuperClassProtectedField;
        }

        public void setAbstractMappedSuperClassProtectedField(Long abstractMappedSuperClassProtectedField) {
            this.abstractMappedSuperClassProtectedField = abstractMappedSuperClassProtectedField;
        }

        public Long nonStandardGetterForAbstractMappedSuperClassProtectedField() {
            return abstractMappedSuperClassProtectedField;
        }

        public void nonStandardSetterForAbstractMappedSuperClassProtectedField(Long value) {
            abstractMappedSuperClassProtectedField = value;
        }

        public Long getAbstractMappedSuperClassPackagePrivateField() {
            return abstractMappedSuperClassPackagePrivateField;
        }

        public void setAbstractMappedSuperClassPackagePrivateField(Long abstractMappedSuperClassPackagePrivateField) {
            this.abstractMappedSuperClassPackagePrivateField = abstractMappedSuperClassPackagePrivateField;
        }

        public Long nonStandardGetterForAbstractMappedSuperClassPackagePrivateField() {
            return abstractMappedSuperClassPackagePrivateField;
        }

        public void nonStandardSetterForAbstractMappedSuperClassPackagePrivateField(Long value) {
            abstractMappedSuperClassPackagePrivateField = value;
        }

        public Long getAbstractEntityPublicField() {
            return abstractEntityPublicField;
        }

        public void setAbstractEntityPublicField(Long abstractEntityPublicField) {
            this.abstractEntityPublicField = abstractEntityPublicField;
        }

        public Long nonStandardGetterForAbstractEntityPublicField() {
            return abstractEntityPublicField;
        }

        public void nonStandardSetterForAbstractEntityPublicField(Long value) {
            abstractEntityPublicField = value;
        }

        public Long getAbstractEntityProtectedField() {
            return abstractEntityProtectedField;
        }

        public void setAbstractEntityProtectedField(Long abstractEntityProtectedField) {
            this.abstractEntityProtectedField = abstractEntityProtectedField;
        }

        public Long nonStandardGetterForAbstractEntityProtectedField() {
            return abstractEntityProtectedField;
        }

        public void nonStandardSetterForAbstractEntityProtectedField(Long value) {
            abstractEntityProtectedField = value;
        }

        public Long getAbstractEntityPackagePrivateField() {
            return abstractEntityPackagePrivateField;
        }

        public void setAbstractEntityPackagePrivateField(Long abstractEntityPackagePrivateField) {
            this.abstractEntityPackagePrivateField = abstractEntityPackagePrivateField;
        }

        public Long nonStandardGetterForAbstractEntityPackagePrivateField() {
            return abstractEntityPackagePrivateField;
        }

        public void nonStandardSetterForAbstractEntityPackagePrivateField(Long value) {
            abstractEntityPackagePrivateField = value;
        }

        public Long nonStandardGetterForPublicField() {
            return publicField;
        }

        public void nonStandardSetterForPublicField(Long value) {
            publicField = value;
        }

        public Long nonStandardGetterForPackagePrivateField() {
            return packagePrivateField;
        }

        public void nonStandardSetterForPackagePrivateField(Long value) {
            packagePrivateField = value;
        }

        public Long nonStandardGetterForProtectedField() {
            return protectedField;
        }

        public void nonStandardSetterForProtectedField(Long value) {
            protectedField = value;
        }

        public Long nonStandardGetterForPrivateField() {
            return privateField;
        }

        public void nonStandardSetterForPrivateField(Long value) {
            privateField = value;
        }

        public static Long staticGetPublicField(MyConcreteEntity entity) {
            return entity.publicField;
        }

        public static void staticSetPublicField(MyConcreteEntity entity, Long value) {
            entity.publicField = value;
        }

        public static Long staticGetProtectedField(MyConcreteEntity entity) {
            return entity.protectedField;
        }

        public static void staticSetProtectedField(MyConcreteEntity entity, Long value) {
            entity.protectedField = value;
        }

        public static Long staticGetPackagePrivateField(MyConcreteEntity entity) {
            return entity.packagePrivateField;
        }

        public static void staticSetPackagePrivateField(MyConcreteEntity entity, Long value) {
            entity.packagePrivateField = value;
        }

        public static Long staticGetPrivateField(MyConcreteEntity entity) {
            return entity.privateField;
        }

        public static void staticSetPrivateField(MyConcreteEntity entity, Long value) {
            entity.privateField = value;
        }

        public static final class InnerClassAccessors {
            public static Long staticGetPublicField(MyConcreteEntity entity) {
                return entity.publicField;
            }

            public static void staticSetPublicField(MyConcreteEntity entity, Long value) {
                entity.publicField = value;
            }

            public static Long staticGetProtectedField(MyConcreteEntity entity) {
                return entity.protectedField;
            }

            public static void staticSetProtectedField(MyConcreteEntity entity, Long value) {
                entity.protectedField = value;
            }

            public static Long staticGetPackagePrivateField(MyConcreteEntity entity) {
                return entity.packagePrivateField;
            }

            public static void staticSetPackagePrivateField(MyConcreteEntity entity, Long value) {
                entity.packagePrivateField = value;
            }

            public static Long staticGetPrivateField(MyConcreteEntity entity) {
                return entity.privateField;
            }

            public static void staticSetPrivateField(MyConcreteEntity entity, Long value) {
                entity.privateField = value;
            }

            public Long instanceGetPublicField(MyConcreteEntity entity) {
                return entity.publicField;
            }

            public void instanceSetPublicField(MyConcreteEntity entity, Long value) {
                entity.publicField = value;
            }

            public Long instanceGetProtectedField(MyConcreteEntity entity) {
                return entity.protectedField;
            }

            public void instanceSetProtectedField(MyConcreteEntity entity, Long value) {
                entity.protectedField = value;
            }

            public Long instanceGetPackagePrivateField(MyConcreteEntity entity) {
                return entity.packagePrivateField;
            }

            public void instanceSetPackagePrivateField(MyConcreteEntity entity, Long value) {
                entity.packagePrivateField = value;
            }

            public Long instanceGetPrivateField(MyConcreteEntity entity) {
                return entity.privateField;
            }

            public void instanceSetPrivateField(MyConcreteEntity entity, Long value) {
                entity.privateField = value;
            }
        }
    }

    public static final class ExternalClassAccessors {
        public static Long staticGetPublicField(MyConcreteEntity entity) {
            return entity.publicField;
        }

        public static void staticSetPublicField(MyConcreteEntity entity, Long value) {
            entity.publicField = value;
        }

        public static Long staticGetProtectedField(MyConcreteEntity entity) {
            return entity.protectedField;
        }

        public static void staticSetProtectedField(MyConcreteEntity entity, Long value) {
            entity.protectedField = value;
        }

        public static Long staticGetPackagePrivateField(MyConcreteEntity entity) {
            return entity.packagePrivateField;
        }

        public static void staticSetPackagePrivateField(MyConcreteEntity entity, Long value) {
            entity.packagePrivateField = value;
        }

        public Long instanceGetPublicField(MyConcreteEntity entity) {
            return entity.publicField;
        }

        public void instanceSetPublicField(MyConcreteEntity entity, Long value) {
            entity.publicField = value;
        }

        public Long instanceGetProtectedField(MyConcreteEntity entity) {
            return entity.protectedField;
        }

        public void instanceSetProtectedField(MyConcreteEntity entity, Long value) {
            entity.protectedField = value;
        }

        public Long instanceGetPackagePrivateField(MyConcreteEntity entity) {
            return entity.packagePrivateField;
        }

        public void instanceSetPackagePrivateField(MyConcreteEntity entity, Long value) {
            entity.packagePrivateField = value;
        }
    }

    private interface AccessDelegate {
        void setValue(MyConcreteEntity entity, Long value);

        Long getValue(MyConcreteEntity entity);
    }
}
