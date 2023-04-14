package io.quarkus.observability.devresource.vmagent;

import java.util.Map;

import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.observability.common.config.ConfigUtils;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.common.config.VMAgentConfig;
import io.quarkus.observability.devresource.ContainerResource;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.observability.testcontainers.VMAgentContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class VMAgentResource extends ContainerResource<VMAgentContainer, VMAgentConfig>
        implements QuarkusTestResourceLifecycleManager {

    private Context context;

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    protected int port() {
        // if context == null, it was run from dev mode
        return context != null || LaunchMode.current() == LaunchMode.TEST ? 8081 : 8080;
    }

    @Override
    public VMAgentConfig config(ModulesConfiguration configuration) {
        return configuration.vmAgent();
    }

    @Override
    public GenericContainer<?> container(VMAgentConfig config, ModulesConfiguration root) {
        String vmEndpoint = ConfigUtils.vmEndpoint(root.victoriaMetrics());
        return set(new VMAgentContainer(config, "http://" + vmEndpoint, config.scrapePort().orElse(port())));
    }

    @Override
    public Map<String, String> config(int privatePort, String host, int publicPort) {
        return Map.of();
    }

    @Override
    protected VMAgentContainer defaultContainer() {
        return new VMAgentContainer("http://victoria-metrics:8428", port());
    }

    @Override
    protected Map<String, String> doStart() {
        return Map.of();
    }

    @Override
    public Map<String, String> start() {
        int port = port();
        Testcontainers.exposeHostPorts(port);
        // TODO - url, VM vs Prometheus
        return super.start();
    }

    @Override
    public int order() {
        return DevResourceLifecycleManager.SCRAPER;
    }

}
