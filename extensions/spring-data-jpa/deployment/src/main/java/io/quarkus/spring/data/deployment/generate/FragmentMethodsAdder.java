package io.quarkus.spring.data.deployment.generate;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

public class FragmentMethodsAdder {

    private final Consumer<String> fragmentImplClassResolvedCallback;
    private final IndexView index;

    public FragmentMethodsAdder(Consumer<String> fragmentImplClassResolvedCallback, IndexView index) {
        this.fragmentImplClassResolvedCallback = fragmentImplClassResolvedCallback;
        this.index = index;
    }

    public void add(ClassCreator classCreator, String generatedClassName,
            List<DotName> customInterfaceNamesToImplement, Map<String, FieldDesc> customImplNameToHandle,
            Set<String> existingMethods) {
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

                String[] methodParameterTypes = new String[methodToImplement.parametersCount()];
                for (int i = 0; i < methodToImplement.parametersCount(); i++) {
                    methodParameterTypes[i] = methodToImplement.parameterType(i).name().toString();
                }

                String methodReturnType = methodToImplement.returnType().name().toString();
                String methodKey = GenerationUtil.methodKey(methodToImplement.name(), methodReturnType,
                        methodParameterTypes);

                if (!existingMethods.contains(methodKey)) {
                    // Build the MethodTypeDesc
                    MethodTypeDesc mtd = GenerationUtil.toMethodTypeDesc(methodReturnType, methodParameterTypes);

                    classCreator.method(methodToImplement.name(), mc -> {
                        mc.setType(mtd);
                        // Declare parameters
                        ParamVar[] params = new ParamVar[methodToImplement.parametersCount()];
                        for (int i = 0; i < methodToImplement.parametersCount(); i++) {
                            params[i] = mc.parameter("p" + i);
                        }

                        mc.body(bc -> {
                            // obtain the bean from the field
                            Expr bean = bc.get(mc.this_().field(
                                    customImplNameToHandle.get(customImplementationClassName)));

                            // Build args list
                            List<Expr> args = new ArrayList<>();
                            for (ParamVar param : params) {
                                args.add(param);
                            }

                            // Build the target MethodDesc for invokeVirtual
                            MethodDesc targetMethod = ClassMethodDesc.of(
                                    ClassDesc.of(customImplementationClassName),
                                    methodToImplement.name(), mtd);

                            // delegate call to bean
                            Expr result = bc.invokeVirtual(targetMethod, bean, args);
                            if (void.class.getName().equals(methodReturnType)) {
                                bc.return_();
                            } else {
                                bc.return_(result);
                            }
                        });
                    });
                    existingMethods.add(methodKey);
                }
            }
        }
    }
}
