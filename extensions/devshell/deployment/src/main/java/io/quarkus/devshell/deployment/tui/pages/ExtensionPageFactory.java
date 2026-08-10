package io.quarkus.devshell.deployment.tui.pages;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.devshell.deployment.DevShellContext;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.Screen;
import io.quarkus.devshell.deployment.tui.pages.extensions.AgroalShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.ArcShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.CacheShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.DatasourceShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.FaultToleranceShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.FlywayShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.GraphQLShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.GrpcShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.HealthShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.HibernateOrmShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.InfoShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.KafkaShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.LiquibaseShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.MicrometerShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.OpenApiShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.ReactiveMessagingShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.RestEndpointsShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.SchedulerShellPage;
import io.quarkus.devshell.deployment.tui.pages.extensions.WebDependencyLocatorShellPage;
import io.quarkus.devshell.deployment.tui.screens.ExtensionsListScreen;

public final class ExtensionPageFactory {

    private static final Logger log = Logger.getLogger(ExtensionPageFactory.class);

    private static final Map<String, Function<ExtensionsListScreen.ExtensionInfo, ExtensionPage>> PAGE_REGISTRY;

    static {
        var map = new HashMap<String, Function<ExtensionsListScreen.ExtensionInfo, ExtensionPage>>();
        map.put("quarkus-arc", ext -> new ArcShellPage());
        map.put("quarkus-agroal", ext -> new AgroalShellPage());
        map.put("quarkus-cache", ext -> new CacheShellPage());
        map.put("quarkus-datasource", ext -> new DatasourceShellPage());
        map.put("quarkus-flyway", ext -> new FlywayShellPage());
        map.put("quarkus-grpc", ext -> new GrpcShellPage());
        map.put("quarkus-hibernate-orm", ext -> new HibernateOrmShellPage());
        map.put("quarkus-info", ext -> new InfoShellPage());
        map.put("quarkus-kafka-client", ext -> new KafkaShellPage());
        map.put("quarkus-liquibase", ext -> new LiquibaseShellPage());
        map.put("quarkus-micrometer", ext -> new MicrometerShellPage());
        map.put("quarkus-rest", ext -> new RestEndpointsShellPage());
        map.put("quarkus-resteasy-reactive", ext -> new RestEndpointsShellPage());
        map.put("quarkus-scheduler", ext -> new SchedulerShellPage());
        map.put("quarkus-smallrye-fault-tolerance", ext -> new FaultToleranceShellPage());
        map.put("quarkus-smallrye-graphql", ext -> new GraphQLShellPage());
        map.put("quarkus-smallrye-health", ext -> new HealthShellPage());
        map.put("quarkus-smallrye-openapi", ext -> new OpenApiShellPage());
        map.put("quarkus-messaging", ext -> new ReactiveMessagingShellPage());
        map.put("quarkus-web-dependency-locator", ext -> new WebDependencyLocatorShellPage());
        PAGE_REGISTRY = Map.copyOf(map);
    }

    private ExtensionPageFactory() {
    }

    public static Screen createPage(ExtensionsListScreen.ExtensionInfo ext, AppContext ctx) {
        String namespace = ext.namespace();
        if (namespace == null || namespace.isEmpty()) {
            return new GenericExtensionPage(ext);
        }

        // SPI-registered pages take priority over the hardcoded registry
        DevShellContext.ShellPageInfo spiPage = DevShellContext.getShellPages().get(namespace);
        if (spiPage != null) {
            Screen page = createFromSpi(spiPage, ext);
            if (page != null) {
                log.debugf("Created SPI-registered shell page for %s", namespace);
                return page;
            }
        }

        // Fall back to hardcoded built-in pages
        Function<ExtensionsListScreen.ExtensionInfo, ExtensionPage> factory = PAGE_REGISTRY.get(namespace);
        if (factory != null) {
            ExtensionPage page = factory.apply(ext);
            log.debugf("Created built-in shell page for %s", namespace);
            return page;
        }

        log.debugf("No shell page for %s, using generic", namespace);
        return new GenericExtensionPage(ext);
    }

    private static Screen createFromSpi(DevShellContext.ShellPageInfo info, ExtensionsListScreen.ExtensionInfo ext) {
        // Custom page class (direct class reference available)
        if (info.customPageClass() != null) {
            try {
                return (Screen) info.customPageClass().getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException e) {
                log.warnf("Failed to instantiate custom shell page %s: %s",
                        info.customPageClass().getName(), e.getMessage());
            }
        }

        // Custom page class (by name, for cross-classloader cases)
        if (info.customPageClassName() != null && !info.customPageClassName().isEmpty() && info.customPageClass() == null) {
            try {
                Class<?> pageClass = Thread.currentThread().getContextClassLoader().loadClass(info.customPageClassName());
                return (Screen) pageClass.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | InvocationTargetException | NoSuchMethodException e) {
                log.warnf("Failed to load custom shell page class %s: %s",
                        info.customPageClassName(), e.getMessage());
            }
        }

        // Provider-based page (uses JSON-RPC to fetch data from the extension's namespace)
        if (info.providerClassName() != null && !info.providerClassName().isEmpty()) {
            return new ProviderBasedPage(ext, info.id());
        }

        return null;
    }
}
