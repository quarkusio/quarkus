package io.quarkus.mongodb.panache.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.mongodb.panache.axle.ReactivePanacheMongoRepository;
import io.quarkus.mongodb.panache.axle.ReactivePanacheMongoRepositoryBase;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public class AxlePanacheMongoRepositoryEnhancer extends PanacheRepositoryEnhancer {
    public final static DotName PANACHE_REPOSITORY_BASE_NAME = DotName
            .createSimple(ReactivePanacheMongoRepositoryBase.class.getName());

    public final static DotName PANACHE_REPOSITORY_NAME = DotName.createSimple(ReactivePanacheMongoRepository.class.getName());

    public AxlePanacheMongoRepositoryEnhancer(IndexView index) {
        super(index, PanacheResourceProcessor.DOTNAME_AXLE_PANACHE_REPOSITORY_BASE);
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
            return AxlePanacheMongoEntityEnhancer.MONGO_OPERATIONS_BINARY_NAME;
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
