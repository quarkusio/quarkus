package io.quarkus.arc.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.inject.CreationException;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
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

        Map<String, Supplier<?>> suppliersMap = new HashMap<>();

        for (SyntheticBeanBuildItem bean : syntheticBeans) {
            if (bean.hasRecorderInstance() && bean.isStaticInit()) {
                configureSyntheticBean(recorder, suppliersMap, beanRegistration, bean);
            }
        }
        // Init the map of bean instances
        recorder.initStaticSupplierBeans(suppliersMap);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @Produce(SyntheticBeansRuntimeInitBuildItem.class)
    @BuildStep
    ServiceStartBuildItem initRuntime(ArcRecorder recorder, List<SyntheticBeanBuildItem> syntheticBeans,
            BeanRegistrationPhaseBuildItem beanRegistration, BuildProducer<BeanConfiguratorBuildItem> configurators) {

        Map<String, Supplier<?>> suppliersMap = new HashMap<>();

        for (SyntheticBeanBuildItem bean : syntheticBeans) {
            if (bean.hasRecorderInstance() && !bean.isStaticInit()) {
                configureSyntheticBean(recorder, suppliersMap, beanRegistration, bean);
            }
        }
        recorder.initRuntimeSupplierBeans(suppliersMap);
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

    private void configureSyntheticBean(ArcRecorder recorder, Map<String, Supplier<?>> suppliersMap,
            BeanRegistrationPhaseBuildItem beanRegistration, SyntheticBeanBuildItem bean) {
        DotName implClazz = bean.configurator().getImplClazz();
        String name = createName(implClazz.toString(), bean.configurator().getQualifiers().toString());
        if (bean.configurator().getRuntimeValue() != null) {
            suppliersMap.put(name, recorder.createSupplier(bean.configurator().getRuntimeValue()));
        } else if (bean.configurator().getSupplier() != null) {
            suppliersMap.put(name, bean.configurator().getSupplier());
        }
        BeanConfigurator<?> configurator = beanRegistration.getContext().configure(implClazz)
                .read(bean.configurator());
        if (bean.hasRecorderInstance()) {
            configurator.creator(creator(name, bean));
        }
        configurator.done();
    }

    private String createName(String beanClass, String qualifiers) {
        return beanClass.replace(".", "_") + "_"
                + HashUtil.sha1(qualifiers);
    }

    private Consumer<MethodCreator> creator(String name, SyntheticBeanBuildItem bean) {
        return new Consumer<MethodCreator>() {
            @Override
            public void accept(MethodCreator m) {
                ResultHandle staticMap = m
                        .readStaticField(FieldDescriptor.of(ArcRecorder.class, "supplierMap", Map.class));
                ResultHandle supplier = m.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Map.class, "get", Object.class, Object.class), staticMap,
                        m.load(name));
                // Throw an exception if no supplier is found
                m.ifNull(supplier).trueBranch().throwException(CreationException.class,
                        createMessage(name, bean));
                ResultHandle result = m.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Supplier.class, "get", Object.class),
                        supplier);
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
