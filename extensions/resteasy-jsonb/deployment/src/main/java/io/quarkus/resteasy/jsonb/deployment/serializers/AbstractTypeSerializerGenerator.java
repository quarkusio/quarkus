package io.quarkus.resteasy.jsonb.deployment.serializers;

import javax.json.stream.JsonGenerator;

import org.jboss.jandex.PrimitiveType;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;

public abstract class AbstractTypeSerializerGenerator implements TypeSerializerGenerator {

    protected abstract void generateNotNull(GenerateContext context);

    @Override
    public void generate(GenerateContext context) {
        if (context.isNullChecked() // null has already been checked in the previous level (when checking whether to write the key or not)
                || (context.getType() instanceof PrimitiveType)) {
            generateNotNull(context);
        } else {
            BytecodeCreator bytecodeCreator = context.getBytecodeCreator();
            BytecodeCreator ifScope = bytecodeCreator.createScope();
            BranchResult currentItemNullBranch = ifScope.ifNull(context.getCurrentItem());

            BytecodeCreator currentItemNull = currentItemNullBranch.trueBranch();
            currentItemNull.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(JsonGenerator.class, "writeNull", JsonGenerator.class),
                    context.getJsonGenerator());
            currentItemNull.breakScope(ifScope);

            BytecodeCreator currentIemNotNull = currentItemNullBranch.falseBranch();
            generateNotNull(context.changeByteCodeCreator(currentIemNotNull));
            currentIemNotNull.breakScope(ifScope);
        }
    }

}
