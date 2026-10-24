package io.quarkus.hibernate.orm.applicationfieldaccess;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.transaction.UserTransaction;

import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Non-regression test for "flexible constructors" (JEP 492) where field writes
 * appear before the {@code super()} call.
 * <p>
 * {@code PUTFIELD} on {@code uninitializedThis} is legal bytecode, but
 * {@code INVOKEVIRTUAL} on it is not. If the bytecode enhancer replaces a
 * pre-super {@code PUTFIELD} with a call to {@code $$_hibernate_write_*()},
 * the JVM rejects the class with a {@link VerifyError}.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/57735">#57735</a>
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-21023">HHH-21023</a>
 */
public class FlexibleConstructorFieldAccessTest {

    private static final String ENTITY_INTERNAL_NAME = "io/quarkus/hibernate/orm/applicationfieldaccess/"
            + "FlexibleConstructorFieldAccessTest$FlexConEntity";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> {
                jar.addClass(TransactionTestUtils.class);
                // Add the entity with a flexible constructor (PUTFIELD before super())
                // instead of the normally-compiled version.
                jar.add(new ByteArrayAsset(generateEntityWithFlexibleConstructor()),
                        ENTITY_INTERNAL_NAME + ".class");
            })
            .withConfigurationResource("application.properties");

    @Inject
    EntityManager em;

    @Inject
    UserTransaction transaction;

    @Test
    public void constructor() {
        FlexConEntity entity = new FlexConEntity("hello");
        assertThat(entity.title).isEqualTo("hello");
    }

    @Test
    public void dirtyTracking() {
        FlexConEntity entity = new FlexConEntity("hello");
        assertThat(entity).isInstanceOf(SelfDirtinessTracker.class);
        SelfDirtinessTracker tracker = (SelfDirtinessTracker) entity;
        tracker.$$_hibernate_clearDirtyAttributes();
        assertThat(tracker.$$_hibernate_hasDirtyAttributes()).isFalse();
        entity.title = "changed";
        assertThat(tracker.$$_hibernate_hasDirtyAttributes()).isTrue();
        assertThat(tracker.$$_hibernate_getDirtyAttributes()).contains("title");
    }

    @Test
    public void persistAndRetrieve() {
        FlexConEntity persisted = new FlexConEntity("hello");
        inTransaction(() -> {
            em.persist(persisted);
        });

        inTransaction(() -> {
            FlexConEntity loaded = em.find(FlexConEntity.class, persisted.id);
            assertThat(loaded).isNotNull();
            assertThat(loaded.title).isEqualTo("hello");
        });
    }

    private void inTransaction(Runnable runnable) {
        TransactionTestUtils.inTransaction(transaction, runnable);
    }

    /**
     * Compile-time placeholder for the entity class.
     * <p>
     * The actual bytecode loaded at runtime comes from
     * {@link #generateEntityWithFlexibleConstructor()}, which moves the
     * field write before the {@code super()} call.
     */
    @Entity(name = "FlexConEntity")
    public static class FlexConEntity {

        @Id
        @GeneratedValue
        public Long id;

        public String title;

        protected FlexConEntity() {
        }

        public FlexConEntity(String title) {
            this.title = title;
        }
    }

    /**
     * Generates bytecode identical to {@link FlexConEntity} except that the
     * {@code (String)} constructor writes the field <em>before</em> calling
     * {@code super()} — a "flexible constructor".
     *
     * <pre>
     * public FlexConEntity(String title) {
     *     this.title = title; // PUTFIELD on uninitializedThis — legal
     *     super();
     * }
     * </pre>
     */
    private static byte[] generateEntityWithFlexibleConstructor() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                ENTITY_INTERNAL_NAME, null, "java/lang/Object", null);

        // Inner-class attribute so the JVM knows this is a static inner class
        cw.visitInnerClass(ENTITY_INTERNAL_NAME,
                "io/quarkus/hibernate/orm/applicationfieldaccess/FlexibleConstructorFieldAccessTest",
                "FlexConEntity", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

        // @Entity(name = "FlexConEntity")
        AnnotationVisitor av = cw.visitAnnotation("Ljakarta/persistence/Entity;", true);
        av.visit("name", "FlexConEntity");
        av.visitEnd();

        // @Id @GeneratedValue public Long id
        FieldVisitor fv = cw.visitField(Opcodes.ACC_PUBLIC, "id", "Ljava/lang/Long;", null, null);
        fv.visitAnnotation("Ljakarta/persistence/Id;", true).visitEnd();
        fv.visitAnnotation("Ljakarta/persistence/GeneratedValue;", true).visitEnd();
        fv.visitEnd();

        // public String title
        cw.visitField(Opcodes.ACC_PUBLIC, "title", "Ljava/lang/String;", null, null).visitEnd();

        // protected FlexConEntity() { super(); }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // public FlexConEntity(String title) { this.title = title; super(); }
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, ENTITY_INTERNAL_NAME, "title", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
