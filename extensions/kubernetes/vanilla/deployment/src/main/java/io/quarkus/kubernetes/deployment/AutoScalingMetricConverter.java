package io.quarkus.kubernetes.deployment;

public class AutoScalingMetricConverter {

    public static io.dekorate.knative.config.AutoscalingMetric convert(AutoScalingMetric metric) {
        if (metric == AutoScalingMetric.concurrency) {
            return io.dekorate.knative.config.AutoscalingMetric.concurrency;
        } else if (metric == AutoScalingMetric.rps) {
            return io.dekorate.knative.config.AutoscalingMetric.rps;
        } else if (metric == AutoScalingMetric.cpu) {
            return io.dekorate.knative.config.AutoscalingMetric.cpu;
        } else {
            throw new IllegalStateException("Failed to map autoscaling metric: " + metric + "!");
        }
    }
}
