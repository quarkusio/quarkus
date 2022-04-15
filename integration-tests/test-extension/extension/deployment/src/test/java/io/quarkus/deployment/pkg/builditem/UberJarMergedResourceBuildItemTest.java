package io.quarkus.deployment.pkg.builditem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.QuarkusProdModeTest;

class UberJarMergedResourceBuildItemTest {

    @RegisterExtension
    static final QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource("application.properties", "microprofile-config.properties")
                    .addClass(UberJarMain.class))
            .setApplicationName("uber-jar-merged")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setExpectExit(true)
            .overrideConfigKey("quarkus.package.type", "uber-jar")
            .setForcedDependencies(
                    Collections.singletonList(
                            // META-INF/cxf/bus-extensions.txt should be present in the cxf-rt-transports-http and cxf-core JARs
                            new AppArtifact("org.apache.cxf", "cxf-rt-transports-http", "3.4.3")));

    @Test
    public void testResourceWasMerged() throws IOException {
        assertThat(runner.getStartupConsoleOutput()).contains("RESOURCES: 1",
                "org.apache.cxf.transport.http.HTTPTransportFactory::true",
                "org.apache.cxf.transport.http.HTTPWSDLExtensionLoader::true:true",
                "org.apache.cxf.transport.http.policy.HTTPClientAssertionBuilder::true:true",
                "org.apache.cxf.transport.http.policy.HTTPServerAssertionBuilder::true:true",
                "org.apache.cxf.transport.http.policy.NoOpPolicyInterceptorProvider::true:true",
                "org.apache.cxf.bus.managers.PhaseManagerImpl:org.apache.cxf.phase.PhaseManager:true",
                "org.apache.cxf.bus.managers.WorkQueueManagerImpl:org.apache.cxf.workqueue.WorkQueueManager:true",
                "org.apache.cxf.bus.managers.CXFBusLifeCycleManager:org.apache.cxf.buslifecycle.BusLifeCycleManager:true",
                "org.apache.cxf.bus.managers.ServerRegistryImpl:org.apache.cxf.endpoint.ServerRegistry:true",
                "org.apache.cxf.bus.managers.EndpointResolverRegistryImpl:org.apache.cxf.endpoint.EndpointResolverRegistry:true",
                "org.apache.cxf.bus.managers.HeaderManagerImpl:org.apache.cxf.headers.HeaderManager:true",
                "org.apache.cxf.service.factory.FactoryBeanListenerManager::true",
                "org.apache.cxf.bus.managers.ServerLifeCycleManagerImpl:org.apache.cxf.endpoint.ServerLifeCycleManager:true",
                "org.apache.cxf.bus.managers.ClientLifeCycleManagerImpl:org.apache.cxf.endpoint.ClientLifeCycleManager:true",
                "org.apache.cxf.bus.resource.ResourceManagerImpl:org.apache.cxf.resource.ResourceManager:true",
                "org.apache.cxf.catalog.OASISCatalogManager:org.apache.cxf.catalog.OASISCatalogManager:true",
                "org.apache.cxf.common.util.ASMHelperImpl:org.apache.cxf.common.util.ASMHelper:true",
                "org.apache.cxf.common.spi.ClassLoaderProxyService:org.apache.cxf.common.spi.ClassLoaderService:true");
        assertThat(runner.getExitCode()).isZero();
    }

    @QuarkusMain
    public static class UberJarMain {

        public static void main(String[] args) throws IOException {
            List<URL> resources = Collections
                    .list(UberJarMain.class.getClassLoader().getResources("META-INF/cxf/bus-extensions.txt"));
            System.out.println("RESOURCES: " + resources.size());
            try (InputStream is = resources.get(0).openStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                reader.lines().forEach(System.out::println);
            }
        }

    }
}
