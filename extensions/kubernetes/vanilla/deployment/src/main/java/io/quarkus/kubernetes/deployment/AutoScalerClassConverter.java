package io.quarkus.kubernetes.deployment;

public class AutoScalerClassConverter {

    public static io.dekorate.knative.config.AutoScalerClass convert(AutoScalerClass clazz) {
        if (clazz == AutoScalerClass.hpa) {
            return io.dekorate.knative.config.AutoScalerClass.hpa;
        } else if (clazz == AutoScalerClass.kpa) {
            return io.dekorate.knative.config.AutoScalerClass.kpa;
        } else {
            throw new IllegalStateException("Failed to map autoscaler class: " + clazz + "!");
        }
    }
}
