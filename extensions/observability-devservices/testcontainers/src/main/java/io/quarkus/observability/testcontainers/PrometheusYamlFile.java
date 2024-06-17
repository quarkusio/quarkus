package io.quarkus.observability.testcontainers;

public class PrometheusYamlFile {

    protected static final String PROMETHEUS_CONFIG = """
                global:
                  scrape_interval: 10s
                  evaluation_interval: 10s
                storage:
                  tsdb:
                    out_of_order_time_window: 10m
                scrape_configs:
                  - job_name: '%s'
                    metrics_path: '%s%s'
                    scrape_interval: 10s
                    static_configs:
                      - targets: ['%s:%d']
            """;

    private String serviceName;
    private String rootPath;
    private String metricsPath;
    private String host;
    private int port;

    public PrometheusYamlFile(String serviceName, String rootPath, String metricsPath, String host, int port) {
        this.serviceName = serviceName;
        this.rootPath = rootPath;
        this.metricsPath = metricsPath;
        this.host = host;
        this.port = port;
    }

    public String createPrometheusYamlFile() {
        return String.format(PROMETHEUS_CONFIG, getServiceName(), getRootPath(), getMetricsPath(), getHost(),
                getPort());
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getMetricsPath() {
        return metricsPath;
    }

    public void setMetricsPath(String metricsPath) {
        this.metricsPath = metricsPath;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
