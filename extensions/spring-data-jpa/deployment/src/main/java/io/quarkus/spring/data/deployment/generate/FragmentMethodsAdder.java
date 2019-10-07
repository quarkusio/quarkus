package io.quarkus.spring.data.deployment.generate;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class FragmentMethodsAdder {

    private final Consumer<String> fragmentImplClassResolvedCallback;
    private final IndexView index;

    public FragmentMethodsAdder(Consumer<String> fragmentImplClassResolvedCallback, IndexView index) {
        this.fragmentImplClassResolvedCallback = fragmentImplClassResolvedCallback;
        this.index = index;
    }

    public void add(ClassCreator classCreator, String generatedClassName,
            List<DotName> customInterfaceNamesToImplement, Map<String, FieldDescriptor> customImplNameToHandle) {
        for (DotName customInterfaceToImplement : customInterfaceNamesToImplement) {
            String customImplementationClassName = FragmentMethodsUtil
                    .getImplementationDotName(customInterfaceToImplement, index).toString();
            fragmentImplClassResolvedCallback.accept(customImplementationClassName);

            ClassInfo customInterfaceToImplementClassInfo = index.getClassByName(customInterfaceToImplement);
            if (customInterfaceToImplementClassInfo == null) {
                throw new IllegalArgumentException("Unable to implement" + customInterfaceToImplement
                        + " because it is not known - please make sure it's part of the Quarkus index");
            }

            for (MethodInfo methodToImplement : customInterfaceToImplementClassInfo.methods()) {
                // methods defined on the interface are implemented by forwarding them to the bean that implements them

                Object[] methodParameterTypes = new Object[methodToImplement.parameters().size()];
                for (int i = 0; i < methodToImplement.parameters().size(); i++) {
                    methodParameterTypes[i] = methodToImplement.parameters().get(i).name().toString();
                }

                String methodReturnType = methodToImplement.returnType().name().toString();

                MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(generatedClassName, methodToImplement.name(),
                        methodReturnType, methodParameterTypes);

                if (!classCreator.getExistingMethods().contains(methodDescriptor)) {
                    try (MethodCreator methodCreator = classCreator.getMethodCreator(methodDescriptor)) {
                        // obtain the bean from Arc
                        ResultHandle bean = methodCreator.readInstanceField(
                                customImplNameToHandle.get(customImplementationClassName), methodCreator.getThis());

                        ResultHandle[] methodParameterHandles = new ResultHandle[methodToImplement.parameters().size()];
                        for (int i = 0; i < methodToImplement.parameters().size(); i++) {
                            methodParameterHandles[i] = methodCreator.getMethodParam(i);
                        }

                        // delegate call to bean
                        ResultHandle result = methodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(customImplementationClassName, methodToImplement.name(),
                                        methodReturnType, methodParameterTypes),
                                bean, methodParameterHandles);
                        if (void.class.getName().equals(methodReturnType)) {
                            methodCreator.returnValue(null);
                        } else {
                            methodCreator.returnValue(result);
                        }
                    }
                }
            }
        }
    }
}
