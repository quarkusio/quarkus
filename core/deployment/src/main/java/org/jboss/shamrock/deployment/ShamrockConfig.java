package org.jboss.shamrock.deployment;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class ShamrockConfig {

    public static final ShamrockConfig INSTANCE = new ShamrockConfig();


    private static final Config config = ConfigProvider.getConfig();

    private static final Map<String, String> configItems = new HashMap<>();
    private static final Map<String, String> reverseMap = new IdentityHashMap<>();

    private ShamrockConfig() {

    }

    public String getConfig(String key, String defaultValue) {
        if (configItems.containsKey(key)) {
            return configItems.get(key);
        }
        Optional<String> opt = config.getOptionalValue(key, String.class);
        if (opt.isPresent()) {
            String retVal = new String(opt.get());
            configItems.put(key, retVal);
            reverseMap.put(retVal, key);
            return retVal;
        } else {
            String retVal = new String(defaultValue);
            configItems.put(key, retVal);
            reverseMap.put(retVal, key);
            return retVal;
        }
    }

    public static String getConfigKey(String val) {
        return reverseMap.get(val);
    }


}
