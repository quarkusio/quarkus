package io.quarkus.panache.common.deployment;

import java.lang.reflect.Modifier;
import java.util.List;

import javax.persistence.Transient;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.panache.common.deployment.visitors.PanacheJpaEntityClassVisitor;

public class PanacheJpaEntityEnhancer extends PanacheEntityEnhancer<MetamodelInfo<EntityModel<EntityField>>> {
    private static final DotName DOTNAME_TRANSIENT = DotName.createSimple(Transient.class.getName());
    private TypeBundle typeBundle;

    public PanacheJpaEntityEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle typeBundle) {
        super(index, methodCustomizers);
        this.typeBundle = typeBundle;
        modelInfo = new MetamodelInfo<>();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheJpaEntityClassVisitor(outputClassVisitor, modelInfo,
                indexView.getClassByName(DotName.createSimple(className)), methodCustomizers, indexView, typeBundle);
    }

    public void collectFields(ClassInfo classInfo) {
        EntityModel<EntityField> entityModel = new EntityModel<>(classInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(DOTNAME_TRANSIENT)) {
                entityModel.addField(new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        modelInfo.addEntityModel(entityModel);
    }
}
