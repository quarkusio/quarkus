package io.quarkus.arc.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.QualifierConfigurator;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class SyntheticBeansProcessor {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void build(ArcRecorder recorder, List<RuntimeBeanBuildItem> runtimeBeans, List<SyntheticBeanBuildItem> syntheticBeans,
            BeanRegistrationPhaseBuildItem beanRegistration, BuildProducer<BeanConfiguratorBuildItem> configurators) {

        Map<String, Supplier<?>> suppliersMap = new HashMap<>();

        for (RuntimeBeanBuildItem bean : runtimeBeans) {
            //deterministic name
            //as we know the maps are sorted this will result in the same hash for the same bean
            String name = createName(bean.type, bean.qualifiers.toString());
            if (bean.runtimeValue != null) {
                suppliersMap.put(name, recorder.createSupplier(bean.runtimeValue));
            } else {
                suppliersMap.put(name, bean.supplier);
            }
            DotName beanClass = DotName.createSimple(bean.type);
            BeanConfigurator<Object> configurator = beanRegistration.getContext().configure(beanClass);
            // Bean types
            configurator.addType(beanClass);
            // Qualifiers
            if (!bean.qualifiers.isEmpty()) {
                for (Map.Entry<String, NavigableMap<String, Object>> entry : bean.qualifiers.entrySet()) {
                    DotName qualifierName = DotName.createSimple(entry.getKey());
                    QualifierConfigurator<BeanConfigurator<Object>> qualifier = configurator.addQualifier()
                            .annotation(qualifierName);
                    if (!entry.getValue().isEmpty()) {
                        for (Entry<String, Object> valEntry : entry.getValue().entrySet()) {
                            qualifier.addValue(valEntry.getKey(), valEntry.getValue());
                        }
                    }
                    qualifier.done();
                }
            }
            configurator.scope(bean.scope);
            if (!bean.removable) {
                configurator.unremovable();
            }
            // Create the bean instance
            configurator.creator(creator(name));
            // Finish the registration
            configurator.done();
        }

        for (SyntheticBeanBuildItem bean : syntheticBeans) {
            DotName implClazz = bean.configurator().getImplClazz();
            String name = createName(implClazz.toString(), bean.configurator().getQualifiers().toString());
            if (bean.configurator().runtimeValue != null) {
                suppliersMap.put(name, recorder.createSupplier(bean.configurator().runtimeValue));
            } else {
                suppliersMap.put(name, bean.configurator().supplier);
            }
            beanRegistration.getContext().configure(implClazz)
                    .read(bean.configurator())
                    .creator(creator(name))
                    .done();
        }

        // Init the map of bean instances
        recorder.initSupplierBeans(suppliersMap);
    }

    private String createName(String beanClass, String qualifiers) {
        return beanClass.replace(".", "_") + "_"
                + HashUtil.sha1(qualifiers);
    }

    private Consumer<MethodCreator> creator(String name) {
        return new Consumer<MethodCreator>() {
            @Override
            public void accept(MethodCreator m) {
                ResultHandle staticMap = m
                        .readStaticField(FieldDescriptor.of(ArcRecorder.class, "supplierMap", Map.class));
                ResultHandle supplier = m.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Map.class, "get", Object.class, Object.class), staticMap,
                        m.load(name));
                ResultHandle result = m.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Supplier.class, "get", Object.class),
                        supplier);
                m.returnValue(result);
            }
        };
    }

}
