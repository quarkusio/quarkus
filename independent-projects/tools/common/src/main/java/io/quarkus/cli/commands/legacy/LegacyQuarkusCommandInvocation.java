package io.quarkus.cli.commands.legacy;

import io.quarkus.cli.commands.QuarkusCommandInvocation;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import java.util.Map;
import java.util.Properties;

/**
 * @deprecated since 1.3.0.CR1
 *             Please use {@link QuarkusCommandInvocation} instead
 */
@Deprecated
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
