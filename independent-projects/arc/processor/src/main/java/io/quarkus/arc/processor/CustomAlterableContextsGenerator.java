package io.quarkus.arc.processor;

import java.util.Collection;

import org.jboss.logging.Logger;

import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.processor.CustomAlterableContexts.CustomAlterableContextInfo;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;

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
        String generatedName = info.generatedName.replace('.', '/');

        ClassCreator injectableContextSubclass = ClassCreator.builder()
                .classOutput(classOutput)
                .className(generatedName)
                .superClass(info.contextClass)
                .interfaces(InjectableContext.class)
                .build();

        // constructor
        MethodCreator constructor = injectableContextSubclass.getMethodCreator(Methods.INIT, void.class);
        constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(info.contextClass), constructor.getThis());
        constructor.returnVoid();

        // implement `isNormal()` if needed
        if (info.isNormal != null) {
            MethodCreator isNormal = injectableContextSubclass.getMethodCreator("isNormal", boolean.class);
            isNormal.returnBoolean(info.isNormal);
        }

        // implement `destroy()`
        MethodCreator destroy = injectableContextSubclass.getMethodCreator("destroy", void.class);
        destroy.throwException(UnsupportedOperationException.class, "Custom AlterableContext cannot destroy all instances");
        destroy.returnVoid();

        // implement `getState()`
        MethodCreator getState = injectableContextSubclass.getMethodCreator("getState", InjectableContext.ContextState.class);
        getState.throwException(UnsupportedOperationException.class, "Custom AlterableContext has no state");
        getState.returnNull();

        // implement `destroy(ContextState)`
        MethodCreator destroyState = injectableContextSubclass.getMethodCreator("destroy", void.class,
                InjectableContext.ContextState.class);
        destroyState.throwException(UnsupportedOperationException.class, "Custom AlterableContext has no state");
        destroyState.returnVoid();

        injectableContextSubclass.close();
        LOGGER.debugf("InjectableContext subclass generated: %s", info.generatedName);
    }
}
