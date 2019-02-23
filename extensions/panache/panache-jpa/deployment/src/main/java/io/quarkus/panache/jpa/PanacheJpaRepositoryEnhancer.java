package io.quarkus.panache.jpa;

import static io.quarkus.panache.jpa.PanacheJpaEntityEnhancer.JPA_OPERATIONS_BINARY_NAME;
import static io.quarkus.panache.jpa.PanacheJpaEntityEnhancer.PARAMETERS_SIGNATURE;
import static io.quarkus.panache.jpa.PanacheJpaEntityEnhancer.QUERY_BINARY_NAME;
import static io.quarkus.panache.jpa.PanacheJpaEntityEnhancer.QUERY_SIGNATURE;
import static io.quarkus.panache.jpa.PanacheJpaEntityEnhancer.SORT_SIGNATURE;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

import io.quarkus.panache.jpa.PanacheRepository;
import io.quarkus.panache.jpa.PanacheRepositoryBase;

public class PanacheJpaRepositoryEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String PANACHE_REPOSITORY_BASE_NAME = PanacheRepositoryBase.class.getName();
    public final static String PANACHE_REPOSITORY_BASE_BINARY_NAME = PANACHE_REPOSITORY_BASE_NAME.replace('.', '/');
    public final static String PANACHE_REPOSITORY_BASE_SIGNATURE = "L" + PANACHE_REPOSITORY_BASE_BINARY_NAME + ";";

    public final static String PANACHE_REPOSITORY_NAME = PanacheRepository.class.getName();
    public final static String PANACHE_REPOSITORY_BINARY_NAME = PANACHE_REPOSITORY_NAME.replace('.', '/');
    public final static String PANACHE_REPOSITORY_SIGNATURE = "L" + PANACHE_REPOSITORY_BINARY_NAME + ";";

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new DaoEnhancingClassVisitor(className, outputClassVisitor);
    }

    static class DaoEnhancingClassVisitor extends ClassVisitor {

        private Type entityType;
        private String entitySignature;
        private String entityBinaryType;
        private String daoBinaryName;

        public DaoEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, outputClassVisitor);
            daoBinaryName = className.replace('.', '/');
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            SignatureReader signatureReader = new SignatureReader(signature);
            DaoTypeFetcher daoTypeFetcher = new DaoTypeFetcher(PANACHE_REPOSITORY_BINARY_NAME);
            signatureReader.accept(daoTypeFetcher);
            if (daoTypeFetcher.foundType == null) {
                daoTypeFetcher = new DaoTypeFetcher(PANACHE_REPOSITORY_BASE_BINARY_NAME);
                signatureReader.accept(daoTypeFetcher);
            }
            entityBinaryType = daoTypeFetcher.foundType;
            entitySignature = "L" + entityBinaryType + ";";
            entityType = Type.getType(entitySignature);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // FIXME: do not add method if already present
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {

            // Bridge for findById
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE,
                    "findById",
                    "(Ljava/lang/Object;)Ljava/lang/Object;",
                    null,
                    null);
            mv.visitParameter("id", 0);
            mv.visitCode();
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    daoBinaryName,
                    "findById",
                    "(Ljava/lang/Object;)" + entitySignature, false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            generateMethod("findById",
                    "(Ljava/lang/Object;)" + entitySignature,
                    null,
                    Opcodes.ARETURN, entityBinaryType, "id");

            // find String Sort? Map|Object[]|Parameters?

            generateMethod("find",
                    "(Ljava/lang/String;[Ljava/lang/Object;)" + QUERY_SIGNATURE,
                    "(Ljava/lang/String;[Ljava/lang/Object;)L" + QUERY_BINARY_NAME + "<" + entitySignature + ">;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("find",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "[Ljava/lang/Object;)" + QUERY_SIGNATURE,
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "[Ljava/lang/Object;)L" + QUERY_BINARY_NAME + "<" + entitySignature
                            + ">;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("find",
                    "(Ljava/lang/String;Ljava/util/Map;)" + QUERY_SIGNATURE,
                    "(Ljava/lang/String;Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)L" + QUERY_BINARY_NAME + "<"
                            + entitySignature + ">;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("find",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "Ljava/util/Map;)" + QUERY_SIGNATURE,
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)L"
                            + QUERY_BINARY_NAME + "<" + entitySignature + ">;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("find",
                    "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")" + QUERY_SIGNATURE,
                    "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")L" + QUERY_BINARY_NAME + "<" + entitySignature + ">;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("find",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE + ")" + QUERY_SIGNATURE,
                    "(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE + ")L" + QUERY_BINARY_NAME + "<"
                            + entitySignature + ">;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            // list String Sort? Map|Object[]|Parameters?

            generateMethod("list",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/List;",
                    "<T:" + entitySignature + ">(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("list",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "[Ljava/lang/Object;)Ljava/util/List;",
                    "<T:" + entitySignature + ">(Ljava/lang/String;" + SORT_SIGNATURE
                            + "[Ljava/lang/Object;)Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("list",
                    "(Ljava/lang/String;Ljava/util/Map;)Ljava/util/List;",
                    "<T:" + entitySignature
                            + ">(Ljava/lang/String;Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("list",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "Ljava/util/Map;)Ljava/util/List;",
                    "<T:" + entitySignature + ">(Ljava/lang/String;" + SORT_SIGNATURE
                            + "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("list",
                    "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")Ljava/util/List;",
                    "<T:" + entitySignature + ">(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("list",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE + ")Ljava/util/List;",
                    "<T:" + entitySignature + ">(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE
                            + ")Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            // stream String Sort? Map|Object[]|Parameters?

            generateMethod("stream",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/stream/Stream;",
                    "<T:" + entitySignature + ">(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("stream",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "[Ljava/lang/Object;)Ljava/util/stream/Stream;",
                    "<T:" + entitySignature + ">(Ljava/lang/String;" + SORT_SIGNATURE
                            + "[Ljava/lang/Object;)Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("stream",
                    "(Ljava/lang/String;Ljava/util/Map;)Ljava/util/stream/Stream;",
                    "<T:" + entitySignature
                            + ">(Ljava/lang/String;Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("stream",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "Ljava/util/Map;)Ljava/util/stream/Stream;",
                    "<T:" + entitySignature + ">(Ljava/lang/String;" + SORT_SIGNATURE
                            + "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("stream",
                    "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")Ljava/util/stream/Stream;",
                    "<T:" + entitySignature + ">(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("stream",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE + ")Ljava/util/stream/Stream;",
                    "<T:" + entitySignature + ">(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE
                            + ")Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            // findAll Sort?

            generateMethod("findAll",
                    "()" + QUERY_SIGNATURE,
                    "()L" + QUERY_BINARY_NAME + "<" + entitySignature + ">;",
                    Opcodes.ARETURN, null);

            generateMethod("findAll",
                    "(" + SORT_SIGNATURE + ")" + QUERY_SIGNATURE,
                    "(" + SORT_SIGNATURE + ")L" + QUERY_BINARY_NAME + "<" + entitySignature + ">;",
                    Opcodes.ARETURN, null, "sort");

            // listAll Sort?

            generateMethod("listAll",
                    "()Ljava/util/List;",
                    "()Ljava/util/List<" + entitySignature + ">;",
                    Opcodes.ARETURN, null);

            generateMethod("listAll",
                    "(" + SORT_SIGNATURE + ")Ljava/util/List;",
                    "(" + SORT_SIGNATURE + ")Ljava/util/List<" + entitySignature + ">;",
                    Opcodes.ARETURN, null, "sort");

            // streamAll Sort?

            generateMethod("streamAll",
                    "()Ljava/util/stream/Stream;",
                    "()Ljava/util/stream/Stream<" + entitySignature + ">;",
                    Opcodes.ARETURN, null);

            generateMethod("streamAll",
                    "(" + SORT_SIGNATURE + ")Ljava/util/stream/Stream;",
                    "(" + SORT_SIGNATURE + ")Ljava/util/stream/Stream<" + entitySignature + ">;",
                    Opcodes.ARETURN, null, "sort");

            // count [String, Map|Object[]|Parameters?]?

            generateMethod("count", "(Ljava/lang/String;[Ljava/lang/Object;)J", null, Opcodes.LRETURN, null, "query", "params");
            generateMethod("count", "(Ljava/lang/String;Ljava/util/Map;)J", null, Opcodes.LRETURN, null, "query", "params");
            generateMethod("count", "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")J", null, Opcodes.LRETURN, null, "query",
                    "params");
            generateMethod("count", "()J", null, Opcodes.LRETURN, null);

            // delete [String, Map|Object[]|Parameters?]?

            generateMethod("delete", "(Ljava/lang/String;[Ljava/lang/Object;)J", null, Opcodes.LRETURN, null, "query",
                    "params");
            generateMethod("delete", "(Ljava/lang/String;Ljava/util/Map;)J", null, Opcodes.LRETURN, null, "query", "params");
            generateMethod("delete", "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")J", null, Opcodes.LRETURN, null, "query",
                    "params");
            generateMethod("deleteAll", "()J", null, Opcodes.LRETURN, null);

            super.visitEnd();
        }

        private void generateMethod(String name, String descriptor, String signature, int returnOpCode, String castTo,
                String... params) {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    name,
                    descriptor,
                    signature,
                    null);
            for (int i = 0; i < params.length; i++) {
                mv.visitParameter(params[i], 0 /* modifiers */);
            }
            mv.visitCode();
            // inject Class
            mv.visitLdcInsn(entityType);
            for (int i = 0; i < params.length; i++) {
                mv.visitIntInsn(Opcodes.ALOAD, i + 1);
            }
            // inject Class
            String forwardingDescriptor = "(Ljava/lang/Class;" + descriptor.substring(1);
            if (castTo != null) {
                // return type is erased to Object
                int lastParen = forwardingDescriptor.lastIndexOf(')');
                forwardingDescriptor = forwardingDescriptor.substring(0, lastParen + 1) + "Ljava/lang/Object;";
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    name,
                    forwardingDescriptor, false);
            if (castTo != null)
                mv.visitTypeInsn(Opcodes.CHECKCAST, castTo);
            mv.visitInsn(returnOpCode);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
}
