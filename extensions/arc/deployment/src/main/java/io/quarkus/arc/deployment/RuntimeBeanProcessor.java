package io.quarkus.arc.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.AnnotationCreator;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class RuntimeBeanProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    RuntimeBeanSupplierMapsBuildItem build(List<RuntimeBeanBuildItem> beans,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            ArcRecorder recorder,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        String beanName = "io.quarkus.arc.runtimebean.RuntimeBeanProducers";

        ClassCreator classCreator = new ClassCreator(new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedBean.produce(new GeneratedBeanBuildItem(name, data));
            }
        }, beanName, null, Object.class.getName());

        classCreator.addAnnotation(ApplicationScoped.class);
        Map<String, Supplier<Object>> staticBeans = new HashMap<>();
        Map<String, Supplier<Object>> runtimeBeans = new HashMap<>();
        for (RuntimeBeanBuildItem bean : beans) {
            Map<String, Supplier<Object>> map = bean.isStaticInit() ? staticBeans : runtimeBeans;
            //deterministic name
            //as we know the maps are sorted this will result in the same hash for the same bean
            String name = bean.type.replace(".", "_") + "_" + HashUtil.sha1(bean.qualifiers.toString());
            if (bean.runtimeValue != null) {
                map.put(name, recorder.createSupplier(bean.runtimeValue));
            } else {
                map.put(name, bean.supplier);
            }

            MethodCreator producer = classCreator.getMethodCreator("produce_" + name, bean.type);
            producer.addAnnotation(Produces.class);
            producer.addAnnotation(bean.scope);
            for (Map.Entry<String, NavigableMap<String, Object>> qualifierEntry : bean.qualifiers.entrySet()) {
                AnnotationCreator builder = producer.addAnnotation(qualifierEntry.getKey());
                for (Map.Entry<String, Object> valueEntry : qualifierEntry.getValue().entrySet()) {
                    builder.addValue(valueEntry.getKey(), valueEntry.getValue());
                }
            }

            if (!bean.removable) {
                unremovableBeans.produce(new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
                    @Override
                    public boolean test(BeanInfo bean) {
                        return bean.isProducerMethod() && bean.getTarget().get().asMethod().name().equals(name);
                    }
                }));
            }

            ResultHandle staticMap = producer
                    .readStaticField(FieldDescriptor.of(ArcRecorder.class, "supplierMap", Map.class));
            ResultHandle supplier = producer.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Map.class, "get", Object.class, Object.class), staticMap, producer.load(name));
            BranchResult branch = producer.ifNull(supplier);
            branch.trueBranch().throwException(IllegalStateException.class,
                    "Bean has not been initialized, have you used a runtime initialized bean at static init time?");
            ResultHandle result = producer.invokeInterfaceMethod(MethodDescriptor.ofMethod(Supplier.class, "get", Object.class),
                    supplier);
            producer.returnValue(result);
        }
        classCreator.close();
        return new RuntimeBeanSupplierMapsBuildItem(staticBeans, runtimeBeans);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void initStatic(RuntimeBeanSupplierMapsBuildItem infoBuildItem,
            ArcRecorder recorder) {
        recorder.initStaticSupplierBeans(infoBuildItem.staticInitBeans);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void initRuntime(RuntimeBeanSupplierMapsBuildItem infoBuildItem,
            ArcRecorder recorder) {
        recorder.initRuntimeSupplierBeans(infoBuildItem.runtimeInitBeans);
    }

    static final class RuntimeBeanSupplierMapsBuildItem extends SimpleBuildItem {
        final Map<String, Supplier<Object>> staticInitBeans;
        final Map<String, Supplier<Object>> runtimeInitBeans;

        RuntimeBeanSupplierMapsBuildItem(Map<String, Supplier<Object>> staticInitBeans,
                Map<String, Supplier<Object>> runtimeInitBeans) {
            this.staticInitBeans = staticInitBeans;
            this.runtimeInitBeans = runtimeInitBeans;
        }
    }
}
