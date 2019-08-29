package io.quarkus.scheduler.runtime;

class SchedulerConfigHolder {

    private static SchedulerRuntimeConfig schedulerRuntimeConfig;
    private static SchedulerBuildTimeConfig schedulerBuildTimeConfig;

    public static void holdBuildtimeConfig(SchedulerBuildTimeConfig schedulerBuildTimeConfig) {
        SchedulerConfigHolder.schedulerBuildTimeConfig = schedulerBuildTimeConfig;
    }

    public static void holdRuntimeConfig(SchedulerRuntimeConfig schedulerRuntimeConfig) {
        SchedulerConfigHolder.schedulerRuntimeConfig = schedulerRuntimeConfig;
    }

    public static SchedulerRuntimeConfig getSchedulerRuntimeConfig() {
        return schedulerRuntimeConfig;
    }

    public static SchedulerBuildTimeConfig getSchedulerBuildTimeConfig() {
        return schedulerBuildTimeConfig;
    }
}
