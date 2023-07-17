package io.quarkus.arc.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.inject.CreationException;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;

public class SyntheticBeansProcessor {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void initStatic(ArcRecorder recorder, List<SyntheticBeanBuildItem> syntheticBeans,
            BeanRegistrationPhaseBuildItem beanRegistration, BuildProducer<BeanConfiguratorBuildItem> configurators) {

        Map<String, Function<SyntheticCreationalContext<?>, ?>> functionsMap = new HashMap<>();

        for (SyntheticBeanBuildItem bean : syntheticBeans) {
            if (bean.hasRecorderInstance() && bean.isStaticInit()) {
                configureSyntheticBean(recorder, functionsMap, beanRegistration, bean);
            }
        }
        // Init the map of bean instances
        recorder.initStaticSupplierBeans(functionsMap);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @Produce(SyntheticBeansRuntimeInitBuildItem.class)
    @BuildStep
    ServiceStartBuildItem initRuntime(ArcRecorder recorder, List<SyntheticBeanBuildItem> syntheticBeans,
            BeanRegistrationPhaseBuildItem beanRegistration, BuildProducer<BeanConfiguratorBuildItem> configurators) {

        Map<String, Function<SyntheticCreationalContext<?>, ?>> functionsMap = new HashMap<>();

        for (SyntheticBeanBuildItem bean : syntheticBeans) {
            if (bean.hasRecorderInstance() && !bean.isStaticInit()) {
                configureSyntheticBean(recorder, functionsMap, beanRegistration, bean);
            }
        }
        recorder.initRuntimeSupplierBeans(functionsMap);
        return new ServiceStartBuildItem("runtime-bean-init");
    }

    @BuildStep
    void initRegular(List<SyntheticBeanBuildItem> syntheticBeans,
            BeanRegistrationPhaseBuildItem beanRegistration, BuildProducer<BeanConfiguratorBuildItem> configurators) {

        for (SyntheticBeanBuildItem bean : syntheticBeans) {
            if (!bean.hasRecorderInstance()) {
                configureSyntheticBean(null, null, beanRegistration, bean);
            }
        }
    }

    private void configureSyntheticBean(ArcRecorder recorder,
            Map<String, Function<SyntheticCreationalContext<?>, ?>> functionsMap,
            BeanRegistrationPhaseBuildItem beanRegistration, SyntheticBeanBuildItem bean) {
        String name = createName(bean.configurator());
        if (bean.configurator().getRuntimeValue() != null) {
            functionsMap.put(name, recorder.createFunction(bean.configurator().getRuntimeValue()));
        } else if (bean.configurator().getSupplier() != null) {
            functionsMap.put(name, recorder.createFunction(bean.configurator().getSupplier()));
        } else if (bean.configurator().getFunction() != null) {
            functionsMap.put(name, bean.configurator().getFunction());
        } else if (bean.configurator().getRuntimeProxy() != null) {
            functionsMap.put(name, recorder.createFunction(bean.configurator().getRuntimeProxy()));
        }
        BeanConfigurator<?> configurator = beanRegistration.getContext().configure(bean.configurator().getImplClazz())
                .read(bean.configurator());
        if (bean.hasRecorderInstance()) {
            configurator.creator(creator(name, bean));
        }
        configurator.done();
    }

    private String createName(ExtendedBeanConfigurator configurator) {
        return configurator.getImplClazz().toString().replace(".", "_") + "_"
                + HashUtil.sha1(configurator.getTypes().toString() + configurator.getQualifiers().toString());
    }

    private Consumer<MethodCreator> creator(String name, SyntheticBeanBuildItem bean) {
        return new Consumer<MethodCreator>() {
            @Override
            public void accept(MethodCreator m) {
                ResultHandle staticMap = m
                        .readStaticField(FieldDescriptor.of(ArcRecorder.class, "syntheticBeanProviders", Map.class));
                ResultHandle function = m.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Map.class, "get", Object.class, Object.class), staticMap,
                        m.load(name));
                // Throw an exception if no supplier is found
                m.ifNull(function).trueBranch().throwException(CreationException.class,
                        createMessage(name, bean));
                ResultHandle result = m.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Function.class, "apply", Object.class, Object.class),
                        function, m.getMethodParam(0));
                m.returnValue(result);
            }
        };
    }

    private String createMessage(String name, SyntheticBeanBuildItem bean) {
        StringBuilder builder = new StringBuilder();
        builder.append("Synthetic bean instance for ");
        builder.append(bean.configurator().getImplClazz());
        builder.append(" not initialized yet: ");
        builder.append(name);
        if (!bean.isStaticInit()) {
            builder.append("\n\t- a synthetic bean initialized during RUNTIME_INIT must not be accessed during STATIC_INIT");
            builder.append(
                    "\n\t- RUNTIME_INIT build steps that require access to synthetic beans initialized during RUNTIME_INIT should consume the SyntheticBeansRuntimeInitBuildItem");
        }
        return builder.toString();
    }

}
