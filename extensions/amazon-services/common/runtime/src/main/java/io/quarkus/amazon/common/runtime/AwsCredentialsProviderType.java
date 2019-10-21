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
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
            return DefaultCredentialsProvider.builder()
                    .asyncCredentialUpdateEnabled(config.defaultProvider.asyncCredentialUpdateEnabled)
                    .reuseLastProviderEnabled(config.defaultProvider.reuseLastProviderEnabled).build();
        }
    },
    STATIC {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.staticProvider.accessKeyId.get(),
                            config.staticProvider.secretAccessKey.get()));
        }
    },

    SYSTEM_PROPERTY {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
            return SystemPropertyCredentialsProvider.create();
        }
    },
    ENV_VARIABLE {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
            return EnvironmentVariableCredentialsProvider.create();
        }
    },
    PROFILE {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
            ProfileCredentialsProviderConfig cfg = config.profileProvider;
            ProfileCredentialsProvider.Builder builder = ProfileCredentialsProvider.builder();
            cfg.profileName.ifPresent(builder::profileName);

            return builder.build();
        }
    },
    CONTAINER {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
            return ContainerCredentialsProvider.builder().build();
        }
    },
    INSTANCE_PROFILE {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
            return InstanceProfileCredentialsProvider.builder().build();
        }
    },
    PROCESS {
        @Override
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
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
        public AwsCredentialsProvider create(AwsCredentialsProviderConfig config) {
            return AnonymousCredentialsProvider.create();
        }
    };

    public abstract AwsCredentialsProvider create(AwsCredentialsProviderConfig config);
}
