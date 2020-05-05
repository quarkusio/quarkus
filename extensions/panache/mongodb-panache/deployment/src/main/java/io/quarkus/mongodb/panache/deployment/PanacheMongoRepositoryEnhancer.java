package io.quarkus.mongodb.panache.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public class PanacheMongoRepositoryEnhancer extends PanacheRepositoryEnhancer {
    public final static DotName PANACHE_REPOSITORY_BASE_NAME = DotName.createSimple(PanacheMongoRepositoryBase.class.getName());

    public final static DotName PANACHE_REPOSITORY_NAME = DotName.createSimple(PanacheMongoRepository.class.getName());

    public PanacheMongoRepositoryEnhancer(IndexView index) {
        super(index, PanacheResourceProcessor.DOTNAME_PANACHE_REPOSITORY_BASE);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheMongoRepositoryClassVisitor(className, outputClassVisitor, panacheRepositoryBaseClassInfo,
                this.indexView);
    }

    static class PanacheMongoRepositoryClassVisitor extends PanacheRepositoryClassVisitor {

        public PanacheMongoRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor,
                ClassInfo panacheRepositoryBaseClassInfo, IndexView indexView) {
            super(className, outputClassVisitor, panacheRepositoryBaseClassInfo, indexView);
        }

        @Override
        protected DotName getPanacheRepositoryDotName() {
            return PANACHE_REPOSITORY_NAME;
        }

        @Override
        protected DotName getPanacheRepositoryBaseDotName() {
            return PANACHE_REPOSITORY_BASE_NAME;
        }

        @Override
        protected String getPanacheOperationsBinaryName() {
            return PanacheMongoEntityEnhancer.MONGO_OPERATIONS_BINARY_NAME;
        }

        @Override
        protected void injectModel(MethodVisitor mv) {
            // inject Class
            mv.visitLdcInsn(entityType);
        }

        @Override
        protected String getModelDescriptor() {
            return "Ljava/lang/Class;";
        }
    }
}
