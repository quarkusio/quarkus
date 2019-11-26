package io.quarkus.cli.commands.legacy;

import java.util.Map;
import java.util.Properties;

import io.quarkus.cli.commands.QuarkusCommandInvocation;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

public class LegacyQuarkusCommandInvocation extends QuarkusCommandInvocation {

    public LegacyQuarkusCommandInvocation() {
        this(null);
    }

    public LegacyQuarkusCommandInvocation(Map<String, Object> params) {
        super(QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor(),
                QuarkusPlatformConfig.getGlobalDefault().getMessageWriter(),
                params,
                new Properties(System.getProperties()));
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getValue() != null) {
                    setProperty(entry.getKey(), entry.getValue().toString());
                }
            }
        }
    }
}
