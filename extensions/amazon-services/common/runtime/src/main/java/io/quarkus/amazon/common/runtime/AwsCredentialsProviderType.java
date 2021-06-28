package io.quarkus.amazon.common.runtime;

import io.quarkus.amazon.common.runtime.AwsCredentialsProviderConfig.ProfileCredentialsProviderConfig;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProcessCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;

public enum AwsCredentialsProviderType {
    DEFAULT {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return DefaultCredentialsProvider.builder()
                    .asyncCredentialUpdateEnabled(config.defaultProvider.asyncCredentialUpdateEnabled)
                    .reuseLastProviderEnabled(config.defaultProvider.reuseLastProviderEnabled).build();
        }
    },
    STATIC {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            if (!config.staticProvider.accessKeyId.isPresent()
                    || !config.staticProvider.secretAccessKey.isPresent()) {
                throw new RuntimeConfigurationError(
                        String.format("%1$s.aws.credentials.static-provider.access-key-id and "
                                + "%1$s.aws.credentials.static-provider.secret-access-key cannot be empty if STATIC credentials provider used.",
                                configKeyRoot));
            }
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.staticProvider.accessKeyId.get(),
                            config.staticProvider.secretAccessKey.get()));
        }
    },

    SYSTEM_PROPERTY {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return SystemPropertyCredentialsProvider.create();
        }
    },
    ENV_VARIABLE {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return EnvironmentVariableCredentialsProvider.create();
        }
    },
    PROFILE {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            ProfileCredentialsProviderConfig cfg = config.profileProvider;
            ProfileCredentialsProvider.Builder builder = ProfileCredentialsProvider.builder();
            cfg.profileName.ifPresent(builder::profileName);

            return builder.build();
        }
    },
    CONTAINER {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return ContainerCredentialsProvider.builder().build();
        }
    },
    INSTANCE_PROFILE {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return InstanceProfileCredentialsProvider.builder().build();
        }
    },
    PROCESS {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            if (!config.processProvider.command.isPresent()) {
                throw new RuntimeConfigurationError(
                        String.format(
                                "%s.aws.credentials.process-provider.command cannot be empty if PROCESS credentials provider used.",
                                configKeyRoot));
            }
            ProcessCredentialsProvider.Builder builder = ProcessCredentialsProvider.builder()
                    .asyncCredentialUpdateEnabled(config.processProvider.asyncCredentialUpdateEnabled);

            builder.credentialRefreshThreshold(config.processProvider.credentialRefreshThreshold);
            builder.processOutputLimit(config.processProvider.processOutputLimit.asLongValue());
            builder.command(config.processProvider.command.get());

            return builder.build();
        }
    },
    ANONYMOUS {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot) {
            return AnonymousCredentialsProvider.create();
        }
    };

    @Deprecated
    public final AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
        return create(config, "");
    }

    public abstract AwsCredentialsProvider create(AwsCredentialsProviderConfig config, String configKeyRoot);
}
