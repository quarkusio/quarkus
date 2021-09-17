package io.quarkus.micrometer.deployment.binder.mpmetrics;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.micrometer.runtime.binder.mpmetrics.AnnotatedGaugeAdapter;

/**
 * Create beans to handle <code>@Gauge</code> annotations.
 * This is a static utility class, it stores no state. It is ok to import and use
 * classes that reference MP Metrics classes.
 */
public class GaugeAnnotationHandler {
    private static final Logger log = Logger.getLogger(GaugeAnnotationHandler.class);

    static void processAnnotatedGauges(IndexView index, ClassOutput classOutput) {

        Set<String> createdClasses = new HashSet<>();

        // @Gauge applies to methods
        // It creates a callback the method or field on single object instance
        for (AnnotationInstance annotation : index.getAnnotations(MetricDotNames.GAUGE_ANNOTATION)) {
            AnnotationTarget target = annotation.target();
            MethodInfo methodInfo = target.asMethod();
            ClassInfo classInfo = methodInfo.declaringClass();

            // Annotated Gauges can only be used on single-instance beans
            verifyGaugeScope(target, classInfo);

            // Create a GaugeAdapter bean that uses the instance of the bean and invokes the callback
            final String generatedClassName = String.format("%s_%s_GaugeAdapter",
                    classInfo.name().toString(), methodInfo.name().toString());
            if (createdClasses.add(generatedClassName)) {
                createClass(index, classOutput, generatedClassName, annotation, target, classInfo, methodInfo);
            }
        }
    }

    /**
     * Given this Widget class:
     *
     * <pre>
     * public class Widget  {
     *     private LongAccumulator highestValue = new LongAccumulator(Long::max, 0);
     *
     *     // ... some other things that change the value in the accumulator ...
     *
     *     &#64;Gauge(name = "highestValue", unit = MetricUnits.NONE, description = "Highest observed value.")
     *     public Long highestValue() {
     *         return highestValue.get();
     *     }
     * }
     * <pre>
     *
     * This method will generate a GaugeAdapter to call the annotated method:
     *
     * <pre>
     * public class Widget_GaugeAdapter extends GaugeAdapter.GaugeAdapterImpl implements GaugeAdapter {
     *
     *     &#64;Inject
     *     Widget target;
     *
     *     public Widget_GaugeAdapter() {
     *         // name, description, and tags are created from annotation attributes
     *         // init is a method on the superclass
     *         super(name, description, tags);
     *     }
     *
     *     Number getValue() {
     *         return target.highestValue;
     *     }
     *
     *     Object getValue() {
     *         return getValue();
     *     }
     * }
     * </pre>
     */
    static void createClass(IndexView index, ClassOutput classOutput, String generatedClassName,
            AnnotationInstance annotation, AnnotationTarget target,
            ClassInfo classInfo, MethodInfo methodInfo) {
        final Class<?> gaugeAdapter = AnnotatedGaugeAdapter.class;
        final Class<?> gaugeAdapterImpl = AnnotatedGaugeAdapter.GaugeAdapterImpl.class;

        final MethodDescriptor superInit = MethodDescriptor.ofConstructor(gaugeAdapterImpl,
                String.class, String.class, String.class, String[].class);

        final MethodDescriptor superInitUnit = MethodDescriptor.ofConstructor(gaugeAdapterImpl,
                String.class, String.class, String.class, String.class, String[].class);

        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .superClass(gaugeAdapterImpl)
                .interfaces(gaugeAdapter)
                .build()) {
            if (classInfo.annotations().containsKey("Singleton")) {
                classCreator.addAnnotation(Singleton.class);
            } else {
                classCreator.addAnnotation(ApplicationScoped.class);
            }
            MetricAnnotationInfo gaugeInfo = new MetricAnnotationInfo(annotation, index, classInfo, methodInfo, null);

            FieldCreator fieldCreator = classCreator
                    .getFieldCreator("target", classInfo.name().toString())
                    .setModifiers(0); // package private
            fieldCreator.addAnnotation(Inject.class);

            // Create the constructor
            try (MethodCreator mc = classCreator.getMethodCreator("<init>", void.class)) {
                mc.setModifiers(Modifier.PUBLIC);

                ResultHandle tagsHandle = mc.newArray(String.class, gaugeInfo.tags.length);
                for (int i = 0; i < gaugeInfo.tags.length; i++) {
                    mc.writeArrayValue(tagsHandle, i, mc.load(gaugeInfo.tags[i]));
                }

                if (gaugeInfo.unit == null) {
                    // super(name, description, targetName, tags)
                    mc.invokeSpecialMethod(superInit, mc.getThis(),
                            mc.load(gaugeInfo.name),
                            mc.load(gaugeInfo.description),
                            mc.load(methodInfo.toString()),
                            tagsHandle);
                } else {
                    // super(name, description, targetName. unit, tags)
                    mc.invokeSpecialMethod(superInitUnit, mc.getThis(),
                            mc.load(gaugeInfo.name),
                            mc.load(gaugeInfo.description),
                            mc.load(methodInfo.toString()),
                            mc.load(gaugeInfo.unit),
                            tagsHandle);
                }
                mc.returnValue(null);
            }

            // This is the magic: this is the method that forwards to the target object instance
            MethodDescriptor getNumberValue = null;
            try (MethodCreator mc = classCreator.getMethodCreator("getValue", Number.class)) {
                mc.setModifiers(Modifier.PUBLIC);
                ResultHandle targetInstance = mc.readInstanceField(fieldCreator.getFieldDescriptor(), mc.getThis());
                mc.returnValue(mc.invokeVirtualMethod(target.asMethod(), targetInstance));
                getNumberValue = mc.getMethodDescriptor();
            }

            // This is the unresolved-generic form of this argument (from the original templated interface)
            try (MethodCreator generic = classCreator.getMethodCreator("getValue", Object.class)) {
                generic.setModifiers(Modifier.PUBLIC);
                generic.returnValue(generic.invokeVirtualMethod(getNumberValue, generic.getThis()));
                generic.getMethodDescriptor();
            }
        }
    }

    static private void verifyGaugeScope(AnnotationTarget target, ClassInfo classInfo) {
        if (!MetricDotNames.isSingleInstance(classInfo)) {
            log.errorf("Bean %s declares a org.eclipse.microprofile.metrics.annotation.Gauge " +
                    "but is of a scope that may create multiple instances of a bean. " +
                    "@Gauge annotations establish a callback to a single instance. Only use " +
                    "the @Gauge annotation on @ApplicationScoped or @Singleton beans, " +
                    "or in JAX-RS endpoints.",
                    classInfo.name().toString());
            throw new DeploymentException(classInfo.name().toString() +
                    " uses a @Gauge annotation, but is not @ApplicationScoped, a @Singleton, or a REST endpoint");
        }
    }
}
