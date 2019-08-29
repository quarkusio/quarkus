package io.quarkus.dynamodb.runtime;

import io.quarkus.dynamodb.runtime.AwsCredentialsProviderConfig.ProfileCredentialsProviderConfig;
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
                    AwsBasicCredentials.create(config.staticProvider.accessKeyId, config.staticProvider.secretAccessKey));
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

            config.processProvider.credentialRefreshThreshold.ifPresent(builder::credentialRefreshThreshold);
            config.processProvider.processOutputLimit.ifPresent(builder::processOutputLimit);
            builder.command(config.processProvider.command);

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
