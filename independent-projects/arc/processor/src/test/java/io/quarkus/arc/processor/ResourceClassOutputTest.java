package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.desc.MethodDesc;

public class ResourceClassOutputTest {

    @Test
    public void testBootstrapMethodsAreRecorded() {
        ResourceClassOutput output = new ResourceClassOutput(true, false);
        ClassDesc withLambda = ClassDesc.of("io.quarkus.arc.test.WithLambda");
        ClassDesc withoutLambda = ClassDesc.of("io.quarkus.arc.test.WithoutLambda");

        AbstractGenerator.gizmo(output).class_(withLambda, cc -> {
            cc.staticMethod("get", mc -> {
                mc.returning(Object.class);
                mc.body(b0 -> {
                    Expr supplier = b0.lambda(Supplier.class, lc -> lc.body(b1 -> b1.return_(Const.of("hello"))));
                    b0.return_(b0.invokeInterface(MethodDesc.of(Supplier.class, "get", Object.class), supplier));
                });
            });
        });
        AbstractGenerator.gizmo(output).class_(withoutLambda, cc -> {
        });

        List<Resource> resources = output.getResources();
        assertEquals(2, resources.size());

        Resource withLambdaResource = resources.get(0);
        assertEquals("io.quarkus.arc.test.WithLambda", withLambdaResource.getFullyQualifiedName());
        assertFalse(withLambdaResource.getBootstrapMethods().isEmpty());
        for (Resource.BootstrapMethod bootstrap : withLambdaResource.getBootstrapMethods()) {
            // a bootstrap method always takes a `MethodHandles.Lookup` first
            assertEquals(ConstantDescs.CD_MethodHandles_Lookup.descriptorString(),
                    bootstrap.type().parameterType(0).descriptorString());
        }

        assertTrue(resources.get(1).getBootstrapMethods().isEmpty());
    }
}
