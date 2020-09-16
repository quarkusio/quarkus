package io.quarkus.amazon.common.runtime;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.utils.StringUtils;

@Recorder
public class AmazonClientRecorder {
    private static final Log LOG = LogFactory.getLog(AmazonClientRecorder.class);

    public RuntimeValue<AwsClientBuilder> configure(RuntimeValue<? extends AwsClientBuilder> clientBuilder,
            RuntimeValue<AwsConfig> awsConfig, RuntimeValue<SdkConfig> sdkConfig, SdkBuildTimeConfig sdkBuildTimeConfig,
            String awsServiceName) {
        AwsClientBuilder builder = clientBuilder.getValue();

        initAwsClient(builder, awsServiceName, awsConfig.getValue());
        initSdkClient(builder, awsServiceName, sdkConfig.getValue(), sdkBuildTimeConfig);

        return new RuntimeValue<>(builder);
    }

    public void initAwsClient(AwsClientBuilder builder, String extension, AwsConfig config) {
        config.region.ifPresent(builder::region);

        if (config.credentials.type == AwsCredentialsProviderType.STATIC) {
            if (!config.credentials.staticProvider.accessKeyId.isPresent()
                    || !config.credentials.staticProvider.secretAccessKey.isPresent()) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.aws.credentials.static-provider.access-key-id and "
                                + "quarkus.%s.aws.credentials.static-provider.secret-access-key cannot be empty if STATIC credentials provider used.",
                                extension, extension));
            }
        }
        if (config.credentials.type == AwsCredentialsProviderType.PROCESS) {
            if (!config.credentials.processProvider.command.isPresent()) {
                throw new RuntimeConfigurationError(
                        String.format(
                                "quarkus.%s.aws.credentials.process-provider.command cannot be empty if PROCESS credentials provider used.",
                                extension));
            }
        }

        builder.credentialsProvider(config.credentials.type.create(config.credentials));
    }

    public void initSdkClient(SdkClientBuilder builder, String extension, SdkConfig config, SdkBuildTimeConfig buildConfig) {
        if (config.endpointOverride.isPresent()) {
            URI endpointOverride = config.endpointOverride.get();
            if (StringUtils.isBlank(endpointOverride.getScheme())) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.endpoint-override (%s) - scheme must be specified",
                                extension,
                                endpointOverride.toString()));
            }
        }

        config.endpointOverride.filter(URI::isAbsolute).ifPresent(builder::endpointOverride);

        final ClientOverrideConfiguration.Builder overrides = ClientOverrideConfiguration.builder();
        config.apiCallTimeout.ifPresent(overrides::apiCallTimeout);
        config.apiCallAttemptTimeout.ifPresent(overrides::apiCallAttemptTimeout);

        buildConfig.interceptors.orElse(Collections.emptyList()).stream()
                .map(this::createInterceptor)
                .filter(Objects::nonNull)
                .forEach(overrides::addExecutionInterceptor);
        builder.overrideConfiguration(overrides.build());
    }

    private ExecutionInterceptor createInterceptor(Class<?> interceptorClass) {
        try {
            return (ExecutionInterceptor) Class
                    .forName(interceptorClass.getName(), false, Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error("Unable to create interceptor", e);
            return null;
        }
    }
}
