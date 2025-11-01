package io.quarkus.arc.processor;

import java.util.Collection;

import org.jboss.logging.Logger;

import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.processor.CustomAlterableContexts.CustomAlterableContextInfo;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Gizmo;

/**
 * This is an internal companion of {@link CustomAlterableContexts} that handles generating
 * subclasses of given context classes that implement {@code InjectableContext}.
 */
class CustomAlterableContextsGenerator extends AbstractGenerator {
    private static final Logger LOGGER = Logger.getLogger(CustomAlterableContextsGenerator.class);

    CustomAlterableContextsGenerator(boolean generateSources) {
        super(generateSources);
    }

    /**
     * Creator of an {@link CustomAlterableContexts} must call this method at an appropriate point
     * in time and write the result to an appropriate output. If not, the bytecode sequences generated
     * using the result of {@code CustomAlterableContexts.add()} will refer to non-existing classes.
     *
     * @return the generated classes, never {@code null}
     */
    Collection<Resource> generate(CustomAlterableContexts.CustomAlterableContextInfo info) {
        ResourceClassOutput classOutput = new ResourceClassOutput(info.isApplicationClass, generateSources);
        createInjectableContextSubclass(classOutput, info);
        return classOutput.getResources();
    }

    private void createInjectableContextSubclass(ClassOutput classOutput, CustomAlterableContextInfo info) {
        Gizmo gizmo = gizmo(classOutput);

        gizmo.class_(info.generatedName, cc -> {
            cc.extends_(info.contextClass);
            cc.implements_(InjectableContext.class);

            cc.defaultConstructor();

            // implement `isNormal()` if needed
            if (info.isNormal != null) {
                cc.method("isNormal", mc -> {
                    mc.returning(boolean.class);
                    mc.body(bc -> {
                        bc.return_(info.isNormal);
                    });
                });
            }

            // implement `destroy()`
            cc.method("destroy", mc -> {
                mc.returning(void.class);
                mc.body(bc -> {
                    bc.throw_(UnsupportedOperationException.class, "Custom AlterableContext cannot destroy all instances");
                });
            });

            // implement `getState()`
            cc.method("getState", mc -> {
                mc.returning(InjectableContext.ContextState.class);
                mc.body(bc -> {
                    bc.throw_(UnsupportedOperationException.class, "Custom AlterableContext has no state");

                });
            });

            // implement `destroy(ContextState)`
            cc.method("destroy", mc -> {
                mc.returning(void.class);
                mc.parameter("state", InjectableContext.ContextState.class);
                mc.body(bc -> {
                    bc.throw_(UnsupportedOperationException.class, "Custom AlterableContext has no state");
                });
            });
        });

        LOGGER.debugf("InjectableContext subclass generated: %s", info.generatedName);
    }
}
