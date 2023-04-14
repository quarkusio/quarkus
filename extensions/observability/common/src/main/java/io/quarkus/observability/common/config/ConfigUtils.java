package io.quarkus.observability.common.config;

public class ConfigUtils {

    public static boolean isEnabled(ContainerConfig config) {
        if (config != null && config.enabled()) {
            DevTarget target = config.getClass().getAnnotation(DevTarget.class);
            if (target != null) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                try {
                    cl.loadClass(target.value());
                    return true;
                } catch (ClassNotFoundException ignore) {
                }
            }
        }
        return false;
    }

    public static String vmEndpoint(VictoriaMetricsConfig vmc) {
        String host = vmc.networkAliases().map(s -> s.iterator().next()).orElse("victoria-metrics");
        int port = vmc.port();
        return String.format("%s:%s", host, port);
    }

}
