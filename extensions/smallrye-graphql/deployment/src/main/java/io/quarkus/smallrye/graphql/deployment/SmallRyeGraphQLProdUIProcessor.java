package io.quarkus.smallrye.graphql.deployment;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLConfig;
import io.quarkus.smallrye.graphql.runtime.produi.SmallRyeGraphQLProdUIService;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

/**
 * Contributes a read-only Prod UI page for SmallRye GraphQL. The Dev UI card is
 * not reused: it links to the GraphiQL execution client (run queries / mutations
 * / subscriptions) and an assistant-only client generator. A bespoke read-only
 * component showing the schema document and the operation list is provided
 * instead. The steps consume {@link SmallRyeGraphQLInitializedBuildItem} so the
 * page is only contributed when the GraphQL schema is actually built.
 *
 * When GraphiQL is also served in production (only the case when
 * {@code quarkus.smallrye-graphql.ui.always-include=true}), an external link to it is
 * added so operators can reach the interactive UI from the GraphQL card.
 */
public class SmallRyeGraphQLProdUIProcessor {

    SmallRyeGraphQLConfig graphQLConfig;

    // Produced from a zero-input build step: the provider bean registration in the
    // produi extension must not (transitively) depend on Arc/deployment items, or a
    // build-step cycle results. The page step below keeps the schema gating.
    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(SmallRyeGraphQLProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI(SmallRyeGraphQLInitializedBuildItem initialized,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("GraphQL Schema")
                .icon("font-awesome-solid:diagram-project")
                .componentLink("pwc-graphql-schema.js"));

        // GraphiQL is only served in production when always-include is set (mirrors
        // SmallRyeGraphQLProcessor.shouldInclude). When it is, surface a link to it.
        boolean included = launchModeBuildItem.getLaunchMode().isDevOrTest() || graphQLConfig.ui().alwaysInclude();
        if (included) {
            // GraphiQL is served on the main HTTP interface (the route is registered without
            // .management(...)), but Prod UI runs on the management interface, so the link must
            // be an absolute URL to the main interface for the new tab to reach it.
            String uiPath = nonApplicationRootPathBuildItem.resolvePath(graphQLConfig.ui().rootPath());
            page.addPage(Page.externalPageBuilder("GraphiQL UI")
                    .url(mainInterfaceUrl(uiPath))
                    .icon("font-awesome-solid:table-columns")
                    .doNotEmbed(true));
        }

        return page;
    }

    /**
     * Best-effort absolute URL to a path on the main HTTP interface, deduced from build-time
     * config (mirrors {@code NonApplicationRootPathBuildItem.getManagementUrlPrefix} for the main
     * interface). Needed because Prod UI is served on the management interface and cannot reach a
     * main-interface resource with a relative path.
     */
    private static String mainInterfaceUrl(String path) {
        Config config = ConfigProvider.getConfig();
        String host = config.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        if (host == null || host.isBlank() || "0.0.0.0".equals(host) || "::".equals(host)) {
            host = "localhost";
        }
        boolean https = isMainTlsConfigured(config);
        int port = https
                ? config.getOptionalValue("quarkus.http.ssl-port", Integer.class).orElse(8443)
                : config.getOptionalValue("quarkus.http.port", Integer.class).orElse(8080);
        return (https ? "https://" : "http://") + host + ":" + port + path;
    }

    private static boolean isMainTlsConfigured(Config config) {
        if (config.getOptionalValue("quarkus.http.tls-configuration-name", String.class).isPresent()) {
            return true;
        }
        return config.getOptionalValue("quarkus.http.ssl.certificate.files", String.class).isPresent()
                || config.getOptionalValue("quarkus.http.ssl.certificate.key-files", String.class).isPresent()
                || config.getOptionalValue("quarkus.http.ssl.certificate.file", String.class).isPresent()
                || config.getOptionalValue("quarkus.http.ssl.certificate.key-file", String.class).isPresent()
                || config.getOptionalValue("quarkus.http.ssl.certificate.key-store-file", String.class).isPresent();
    }
}
