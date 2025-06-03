package io.quarkus.azure.functions.deployment;

import static com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils.fromAppService;
import static com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils.mergeAppServiceConfig;
import static java.lang.String.format;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployFunctionAppTask;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.Operation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBase;
import com.microsoft.azure.toolkit.lib.common.operation.OperationThreadContext;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.cmd.DeployCommandActionBuildItem;
import io.quarkus.deployment.cmd.DeployCommandDeclarationBuildItem;
import io.quarkus.deployment.cmd.DeployConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.logging.Log;

public class AzureFunctionsDeployCommand {
    private static final Logger log = Logger.getLogger(AzureFunctionsDeployCommand.class);

    private static final String APP_NAME_PATTERN = "[a-zA-Z0-9\\-]{2,60}";
    private static final String RESOURCE_GROUP_PATTERN = "[a-zA-Z0-9._\\-()]{1,90}";
    private static final String APP_SERVICE_PLAN_NAME_PATTERN = "[a-zA-Z0-9\\-]{1,40}";
    private static final String EMPTY_APP_NAME = "Please config the <appName> in pom.xml.";
    private static final String INVALID_APP_NAME = "The app name '%s' is not valid. The <appName> only allow alphanumeric characters, hyphens and cannot start or end in a hyphen.";
    private static final String EMPTY_RESOURCE_GROUP = "Please config the <resourceGroup> in pom.xml.";
    private static final String INVALID_RESOURCE_GROUP_NAME = "The <resourceGroup> only allow alphanumeric characters, periods, underscores, "
            +
            "hyphens and parenthesis and cannot end in a period.";
    private static final String INVALID_SERVICE_PLAN_NAME = "Invalid value for <appServicePlanName>, it need to match the pattern %s";
    private static final String INVALID_SERVICE_PLAN_RESOURCE_GROUP_NAME = "Invalid value for <appServicePlanResourceGroup>, " +
            "it only allow alphanumeric characters, periods, underscores, hyphens and parenthesis and cannot end in a period.";
    private static final String EMPTY_IMAGE_NAME = "Please config the <image> of <runtime> in pom.xml.";
    private static final String INVALID_OS = "The value of <os> is not correct, supported values are: windows, linux and docker.";
    private static final String EXPANDABLE_PRICING_TIER_WARNING = "'%s' may not be a valid pricing tier, " +
            "please refer to https://aka.ms/maven_function_configuration#supported-pricing-tiers for valid values";
    private static final String EXPANDABLE_REGION_WARNING = "'%s' may not be a valid region, " +
            "please refer to https://aka.ms/maven_function_configuration#supported-regions for valid values";
    private static final String EXPANDABLE_JAVA_VERSION_WARNING = "'%s' may not be a valid java version, recommended values are `Java 17` and `Java 21`";

    protected static final String USING_AZURE_ENVIRONMENT = "Using Azure environment: %s.";

    public static final String AZURE_FUNCTIONS = "azure-functions";
    protected static final String SUBSCRIPTION_TEMPLATE = "Subscription: %s(%s)";
    protected static final String SUBSCRIPTION_NOT_FOUND = "Subscription %s was not found in current account.";

    @BuildStep
    public void declare(BuildProducer<DeployCommandDeclarationBuildItem> producer) {
        producer.produce(new DeployCommandDeclarationBuildItem(AZURE_FUNCTIONS));
    }

    @BuildStep
    public void deploy(DeployConfig deployConfig, AzureFunctionsConfig config,
            AzureFunctionsAppNameBuildItem appName,
            OutputTargetBuildItem output,

            BuildProducer<DeployCommandActionBuildItem> producer) throws Exception {
        if (!deployConfig.isEnabled(AZURE_FUNCTIONS)) {
            return;
        }
        validateParameters(config, appName.getAppName());
        setCurrentOperation();
        AzureMessager.setDefaultMessager(new QuarkusAzureMessager());
        Azure.az().config().setLogLevel(HttpLogDetailLevel.NONE.name());
        initAzureAppServiceClient(config);

        final FunctionAppBase<?, ?, ?> target = createOrUpdateResource(
                config.toFunctionAppConfig(subscriptionId, appName.getAppName()));
        Path outputDirectory = output.getOutputDirectory();
        Path functionStagingDir = outputDirectory.resolve(AZURE_FUNCTIONS).resolve(appName.getAppName());

        deployArtifact(functionStagingDir, target);
        producer.produce(new DeployCommandActionBuildItem(AZURE_FUNCTIONS, true));
    }

    private void setCurrentOperation() {
        // Note:
        // This gets rid of some these messages.  Not sure why or how to remove the rest of them yet:
        // default to NULL OperationContext, because operation or its action operation is null:Quarkus
        try {
            Method push = OperationThreadContext.class.getDeclaredMethod("pushOperation", Operation.class);
            push.setAccessible(true);
            OperationBase dummy = new OperationBase() {
                @Override
                public Object getSource() {
                    return null;
                }

                @Override
                public String getId() {
                    return "Quarkus";
                }

                @Override
                public Callable<?> getBody() {
                    throw new RuntimeException("Not Implmented");
                }

                @Override
                public String getType() {
                    return "Quarkus";
                }

                @Override
                public AzureString getDescription() {
                    return AzureString.fromString("Quarkus");
                }
            };
            OperationThreadContext ctx = OperationThreadContext.current();
            push.invoke(ctx, dummy);
        } catch (Exception e) {
        }
    }

    protected void validateParameters(AzureFunctionsConfig config, String appName) throws BuildException {
        // app name
        if (StringUtils.isBlank(appName)) {
            throw new BuildException(EMPTY_APP_NAME);
        }
        if (appName.startsWith("-") || !appName.matches(APP_NAME_PATTERN)) {
            throw new BuildException(format(INVALID_APP_NAME, appName));
        }
        // resource group
        if (StringUtils.isBlank(config.resourceGroup())) {
            throw new BuildException(EMPTY_RESOURCE_GROUP);
        }
        if (config.resourceGroup().endsWith(".") || !config.resourceGroup().matches(RESOURCE_GROUP_PATTERN)) {
            throw new BuildException(INVALID_RESOURCE_GROUP_NAME);
        }
        // asp name & resource group
        if (StringUtils.isNotEmpty(config.appServicePlanName())
                && !config.appServicePlanName().matches(APP_SERVICE_PLAN_NAME_PATTERN)) {
            throw new BuildException(format(INVALID_SERVICE_PLAN_NAME, APP_SERVICE_PLAN_NAME_PATTERN));
        }
        if (config.appServicePlanResourceGroup().isPresent()
                && StringUtils.isNotEmpty(config.appServicePlanResourceGroup().orElse(null))
                &&
                (config.appServicePlanResourceGroup().orElse(null).endsWith(".")
                        || !config.appServicePlanResourceGroup().orElse(null).matches(RESOURCE_GROUP_PATTERN))) {
            throw new BuildException(INVALID_SERVICE_PLAN_RESOURCE_GROUP_NAME);
        }
        // slot name
        /*
         * if (deploymentSlotSetting != null && StringUtils.isEmpty(deploymentSlotSetting.getName())) {
         * throw new BuildException(EMPTY_SLOT_NAME);
         * }
         * if (deploymentSlotSetting != null && !deploymentSlotSetting.getName().matches(SLOT_NAME_PATTERN)) {
         * throw new BuildException(String.format(INVALID_SLOT_NAME, SLOT_NAME_PATTERN));
         * }
         *
         */
        // region
        if (StringUtils.isNotEmpty(config.region()) && Region.fromName(config.region()).isExpandedValue()) {
            log.warn(format(EXPANDABLE_REGION_WARNING, config.region()));
        }
        // os
        if (StringUtils.isNotEmpty(config.runtime().os()) && OperatingSystem.fromString(config.runtime().os()) == null) {
            throw new BuildException(INVALID_OS);
        }
        // java version
        if (StringUtils.isNotEmpty(config.runtime().javaVersion())) {
            log.warn(format(EXPANDABLE_JAVA_VERSION_WARNING, config.runtime().javaVersion()));
        }
        // pricing tier
        if (config.pricingTier().isPresent() && StringUtils.isNotEmpty(config.pricingTier().orElse(null))
                && PricingTier.fromString(config.pricingTier().orElse(null)).isExpandedValue()) {
            log.warn(format(EXPANDABLE_PRICING_TIER_WARNING, config.pricingTier().orElse(null)));
        }
        // docker image
        if (OperatingSystem.fromString(config.runtime().os()) == OperatingSystem.DOCKER
                && StringUtils.isEmpty(config.runtime().image().orElse(null))) {
            throw new BuildException(EMPTY_IMAGE_NAME);
        }
    }

    protected static AzureAppService appServiceClient;

    protected static String subscriptionId;

    protected AzureAppService initAzureAppServiceClient(AzureFunctionsConfig config) throws BuildException {
        if (appServiceClient == null) {
            final Account account = loginAzure(config.auth());
            final List<Subscription> subscriptions = account.getSubscriptions();
            final String targetSubscriptionId = getTargetSubscriptionId(config.subscriptionId().orElse(null), subscriptions,
                    account.getSelectedSubscriptions());
            checkSubscription(subscriptions, targetSubscriptionId);
            com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account()
                    .setSelectedSubscriptions(Collections.singletonList(targetSubscriptionId));
            appServiceClient = Azure.az(AzureAppService.class);
            printCurrentSubscription(appServiceClient);
            this.subscriptionId = targetSubscriptionId;
        }
        return appServiceClient;
    }

    protected static void checkSubscription(List<Subscription> subscriptions, String targetSubscriptionId)
            throws BuildException {
        if (StringUtils.isEmpty(targetSubscriptionId)) {
            return;
        }
        final Optional<Subscription> optionalSubscription = subscriptions.stream()
                .filter(subscription -> StringUtils.equals(subscription.getId(), targetSubscriptionId))
                .findAny();
        if (!optionalSubscription.isPresent()) {
            throw new BuildException(format(SUBSCRIPTION_NOT_FOUND, targetSubscriptionId));
        }
    }

    protected Account loginAzure(AzureFunctionsConfig.AuthConfig auth) {
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            return Azure.az(AzureAccount.class).account();
        }
        final AuthConfiguration authConfig = auth.toAuthConfiguration();
        if (authConfig.getType() == AuthType.DEVICE_CODE) {
            authConfig.setDeviceCodeConsumer(info -> {
                final String message = StringUtils.replace(info.getMessage(), info.getUserCode(),
                        TextUtils.cyan(info.getUserCode()));
                System.out.println(message);
            });
        }
        final AzureEnvironment configEnv = AzureEnvironmentUtils.stringToAzureEnvironment(authConfig.getEnvironment());
        promptAzureEnvironment(configEnv);
        Azure.az(AzureCloud.class).set(configEnv);
        final Account account = Azure.az(AzureAccount.class).login(authConfig, false);
        final AzureEnvironment env = account.getEnvironment();
        final String environmentName = AzureEnvironmentUtils.azureEnvironmentToString(env);
        if (env != AzureEnvironment.AZURE && env != configEnv) {
            log.info(AzureString.format(USING_AZURE_ENVIRONMENT, environmentName));
        }
        printCredentialDescription(account);
        return account;
    }

    protected static void printCredentialDescription(Account account) {
        final AuthType type = account.getType();
        final String username = account.getUsername();
        if (type != null) {
            log.info(AzureString.format("Auth type: %s", type.toString()));
        }
        if (account.isLoggedIn()) {
            final List<Subscription> selectedSubscriptions = account.getSelectedSubscriptions();
            if (CollectionUtils.isNotEmpty(selectedSubscriptions) && selectedSubscriptions.size() == 1) {
                log.info(AzureString.format("Default subscription: %s(%s)", selectedSubscriptions.get(0).getName(),
                        selectedSubscriptions.get(0).getId()));
            }
        }
        if (StringUtils.isNotEmpty(username)) {
            log.info(AzureString.format("Username: %s", username.trim()));
        }
    }

    private static void promptAzureEnvironment(AzureEnvironment env) {
        if (env != null && env != AzureEnvironment.AZURE) {
            log.info(AzureString.format("Auth environment: %s", AzureEnvironmentUtils.azureEnvironmentToString(env)));
        }
    }

    protected String getTargetSubscriptionId(String defaultSubscriptionId,
            List<Subscription> subscriptions,
            List<Subscription> selectedSubscriptions) throws BuildException {
        if (!StringUtils.isBlank(defaultSubscriptionId)) {
            return defaultSubscriptionId;
        }

        if (selectedSubscriptions.size() == 1) {
            return selectedSubscriptions.get(0).getId();
        }

        if (selectedSubscriptions.isEmpty()) {
            throw new BuildException("You account does not have a subscription to deploy to");
        }
        throw new BuildException("You must specify a subscription id to use for deployment as you have more than one");
    }

    protected void printCurrentSubscription(AzureAppService appServiceClient) {
        if (appServiceClient == null) {
            return;
        }
        final List<Subscription> subscriptions = Azure.az(IAzureAccount.class).account().getSelectedSubscriptions();
        final Subscription subscription = subscriptions.get(0);
        if (subscription != null) {
            Log.info(format(SUBSCRIPTION_TEMPLATE, TextUtils.cyan(subscription.getName()),
                    TextUtils.cyan(subscription.getId())));
        }
    }

    protected FunctionAppBase<?, ?, ?> createOrUpdateResource(final FunctionAppConfig config) throws Exception {
        FunctionApp app = Azure.az(AzureFunctions.class).functionApps(config.subscriptionId()).updateOrCreate(config.appName(),
                config.resourceGroup());
        final boolean newFunctionApp = !app.exists();
        AppServiceConfig defaultConfig = !newFunctionApp ? fromAppService(app, app.getAppServicePlan())
                : buildDefaultConfig(config.subscriptionId(),
                        config.resourceGroup(), config.appName());
        mergeAppServiceConfig(config, defaultConfig);
        if (!newFunctionApp && !config.disableAppInsights() && StringUtils.isEmpty(config.appInsightsKey())) {
            // fill ai key from existing app settings
            config.appInsightsKey(app.getAppSettings().get(CreateOrUpdateFunctionAppTask.APPINSIGHTS_INSTRUMENTATION_KEY));
        }
        return new CreateOrUpdateFunctionAppTask(config).doExecute();
    }

    private AppServiceConfig buildDefaultConfig(String subscriptionId, String resourceGroup, String appName) {
        return AppServiceConfig.buildDefaultFunctionConfig(resourceGroup, appName);
    }

    private void deployArtifact(Path functionStagingDir, final FunctionAppBase<?, ?, ?> target) {
        final File file = functionStagingDir.toFile();
        new DeployFunctionAppTask(target, file, null).doExecute();
    }

    public static class QuarkusAzureMessager implements IAzureMessager, IAzureMessage.ValueDecorator {
        @Override
        public boolean show(IAzureMessage message) {
            switch (message.getType()) {
                case ALERT:
                case CONFIRM:
                case WARNING:
                    String content = message.getContent();
                    log.warn(content);
                    return true;
                case ERROR:
                    log.error(message.getContent(), ((Throwable) message.getPayload()));
                    return true;
                case INFO:
                case SUCCESS:
                default:
                    log.info(message.getContent());
                    return true;
            }
        }

        @Override
        public String decorateValue(@Nonnull Object p, @Nullable IAzureMessage message) {
            return TextUtils.cyan(p.toString());
        }
    }

}
