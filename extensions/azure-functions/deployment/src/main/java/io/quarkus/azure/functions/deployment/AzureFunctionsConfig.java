package io.quarkus.azure.functions.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Azure Functions configuration.
 * Most options supported and name similarly to azure-functions-maven-plugin config
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.azure-functions")
public interface AzureFunctionsConfig {

    /**
     * App name for azure function project. This is required setting.
     *
     * Defaults to the base artifact name
     */
    Optional<String> appName();

    /**
     * Azure Resource Group for your Azure Functions
     */
    @WithDefault("quarkus")
    String resourceGroup();

    /**
     * Specifies the region where your Azure Functions will be hosted; default value is westus.
     * <a href=
     * "https://github.com/microsoft/azure-maven-plugins/wiki/Azure-Functions:-Configuration-Details#supported-regions">Valid
     * values</a>
     */
    @WithDefault("westus")
    String region();

    /**
     * Specifies whether to disable application insights for your function app
     */
    @WithDefault("false")
    boolean disableAppInsights();

    /**
     * Specifies the instrumentation key of application insights which will bind to your function app
     */
    Optional<String> appInsightsKey();

    RuntimeConfig runtime();

    AuthConfig auth();

    /**
     * Specifies the name of the existing App Service Plan when you do not want to create a new one.
     */
    @WithDefault("java-functions-app-service-plan")
    String appServicePlanName();

    /**
     * The app service plan resource group.
     */
    Optional<String> appServicePlanResourceGroup();

    /**
     * Azure subscription id. Required only if there are more than one subscription in your account
     */
    Optional<String> subscriptionId();

    /**
     * The pricing tier.
     */
    Optional<String> pricingTier();

    /**
     * Port to run azure function in local runtime.
     * Will default to quarkus.http.test-port or 8081
     */
    Optional<Integer> funcPort();

    /**
     * Config String for local debug
     */
    @WithDefault("transport=dt_socket,server=y,suspend=n,address=5005")
    String localDebugConfig();

    /**
     * Specifies the application settings for your Azure Functions, which are defined in name-value pairs
     */
    @ConfigItem
    @ConfigDocMapKey("setting-name")
    Map<String, String> appSettings = Collections.emptyMap();

    @ConfigGroup
    interface RuntimeConfig {
        /**
         * Valid values are linux, windows, and docker
         */
        @WithDefault("linux")
        String os();

        /**
         * Should be set to at least the minimum Quarkus compatible version
         */
        @WithDefault("17")
        String javaVersion();

        /**
         * URL of docker image if deploying via docker
         */
        Optional<String> image();

        /**
         * If using docker, url of registry
         */
        Optional<String> registryUrl();

    }

    default FunctionAppConfig toFunctionAppConfig(String subscriptionId, String appName) {
        Map<String, String> appSettings = this.appSettings;
        if (appSettings.isEmpty()) {
            appSettings = new HashMap<>();
            appSettings.put("FUNCTIONS_EXTENSION_VERSION", "~4");
        }
        return (FunctionAppConfig) new FunctionAppConfig()
                .disableAppInsights(disableAppInsights())
                .appInsightsKey(appInsightsKey().orElse(null))
                .appInsightsInstance(appInsightsKey().orElse(null))
                .subscriptionId(subscriptionId)
                .resourceGroup(resourceGroup())
                .appName(appName)
                .servicePlanName(appServicePlanName())
                .servicePlanResourceGroup(appServicePlanResourceGroup().orElse(null))
                .pricingTier(getParsedPricingTier(subscriptionId))
                .region(getParsedRegion())
                .runtime(getRuntimeConfig(subscriptionId))
                .appSettings(appSettings);
    }

    private PricingTier getParsedPricingTier(String subscriptionId) {
        return Optional.ofNullable(this.pricingTier().orElse(null)).map(PricingTier::fromString)
                .orElseGet(() -> Optional.ofNullable(getServicePlan(subscriptionId)).map(AppServicePlan::getPricingTier)
                        .orElse(null));
    }

    private AppServicePlan getServicePlan(String subscriptionId) {
        final String servicePlan = this.appServicePlanName();
        final String servicePlanGroup = StringUtils.firstNonBlank(this.appServicePlanResourceGroup().orElse(null),
                this.resourceGroup());
        return StringUtils.isAnyBlank(subscriptionId, servicePlan, servicePlanGroup) ? null
                : Azure.az(AzureAppService.class).plans(subscriptionId).get(servicePlan, servicePlanGroup);
    }

    private com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig getRuntimeConfig(String subscriptionId) {
        final RuntimeConfig runtime = this.runtime();
        if (runtime == null) {
            return null;
        }
        final OperatingSystem os = Optional.ofNullable(runtime.os()).map(OperatingSystem::fromString)
                .orElseGet(
                        () -> Optional.ofNullable(getServicePlan(subscriptionId)).map(AppServicePlan::getOperatingSystem)
                                .orElse(null));
        final com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig result = new com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig()
                .os(os)
                .javaVersion(runtime.javaVersion())
                .image(runtime.image().orElse(null)).registryUrl(runtime.registryUrl().orElse(null));
        return result;
    }

    private Region getParsedRegion() {
        return Optional.ofNullable(region()).map(Region::fromName).orElse(null);
    }

    @ConfigGroup
    interface AuthConfig {

        /**
         * Description of each type can be found
         * <a href="https://github.com/microsoft/azure-maven-plugins/wiki/Authentication">here</a> Valid values are
         * <ul>
         * <li><b>azure_cli</b> Delegates to Azure CLI for login</li>
         * <li><b>managed_identity</b> Requires client to be set</li>
         * <li><b>oauth2</b> Requires tenant to be set</li>
         * <li><b>device_code</b> Requires tenant to be set</li>
         * <li><b>file</b></li> Filesystem path to a property file that defines authentication. Properties supported are
         * <ul>
         * <li><b>type</b> Supports same type values as well as <i>service_principal</i></li>
         * <li><b>client</b></li>
         * <li><b>tenant</b></li>
         * <li><b>key</b> Password for <i>service_principal</i> if using password authentication</li>
         * <li><b>certificate</b> Path to PEM file if using <i>service_principal</i></li>
         * <li><b>certificate-password</b> Password for PEM file if it is password protected and if using
         * <i>service_principal</i></li>
         * <li><b>environment</b> if using <i>service_principal</i></li>
         * </ul>
         * </ul>
         *
         * Defaults to "azure_cli" for authentication
         */
        @WithDefault("azure_cli")
        String type();

        /**
         * Filesystem path to properties file if using <i>file</i> type
         */
        Optional<String> path();

        /**
         * Client or App Id required if using <i>managed_identity</i> type
         */
        Optional<String> client();

        /**
         * Tenant ID required if using <i>oauth2</i> or <i>device_code</i> type
         */
        Optional<String> tenant();

        private static String findValue(Properties props, String key) {
            if (props.contains(key))
                return (String) props.get(key);
            return (String) props.get("quarkus.azure-functions.auth." + key);
        }

        private static AuthConfiguration fromFile(Optional<String> path) {
            try {
                if (path.isEmpty()) {
                    throw new IllegalArgumentException("Path must be set if using file auth config");

                }
                File file = new File(path.get());
                if (!file.exists()) {
                    throw new IllegalArgumentException("Auth config file not found: " + path.get());
                }
                Properties props = new Properties();
                props.load(new FileInputStream(file));
                String typeVal = findValue(props, "type");
                if (typeVal == null) {
                    throw new IllegalArgumentException("Auth config file does not define type: " + path);
                }
                final AuthType type = AuthType.parseAuthType(typeVal);
                final AuthConfiguration authConfiguration = new AuthConfiguration(type);
                authConfiguration.setClient(findValue(props, "client"));
                authConfiguration.setTenant(findValue(props, "tenant"));
                authConfiguration.setCertificate(findValue(props, "certificate"));
                authConfiguration.setCertificatePassword(findValue(props, "certificate-password"));
                authConfiguration.setKey(findValue(props, "key"));

                authConfiguration.setEnvironment(findValue(props, "environment"));
                authConfiguration.setEnvironment(Optional.ofNullable(authConfiguration.getEnvironment())
                        .orElseGet(() -> AzureEnvironmentUtils.azureEnvironmentToString(AzureEnvironment.AZURE)));

                if (authConfiguration.getType() == AuthType.SERVICE_PRINCIPAL) {
                    authConfiguration.validate();
                }

                return authConfiguration;
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        default AuthConfiguration toAuthConfiguration() {
            try {
                if (this.type().equalsIgnoreCase("file")) {
                    return fromFile(this.path());
                }
                final AuthType type = AuthType.parseAuthType(this.type());
                final AuthConfiguration authConfiguration = new AuthConfiguration(type);
                authConfiguration.setClient(client().orElse(null));
                authConfiguration.setTenant(tenant().orElse(null));
                authConfiguration.setEnvironment(AzureEnvironmentUtils.azureEnvironmentToString(AzureEnvironment.AZURE));
                return authConfiguration;
            } catch (InvalidConfigurationException e) {
                throw new IllegalArgumentException(e);
            }
        }

    }
}
