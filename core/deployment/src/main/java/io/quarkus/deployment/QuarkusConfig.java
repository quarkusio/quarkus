/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.deployment;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.deployment.configuration.ConfigurationError;

public final class QuarkusConfig extends SimpleBuildItem {

    public static final QuarkusConfig INSTANCE = new QuarkusConfig();

    private static final Map<Object, String> reverseMap = new IdentityHashMap<>();

    private QuarkusConfig() {
        //not to be constructed
    }

    public static String getString(String configKey, String defaultValue, boolean allowNull) {
        String opt = ConfigProvider.getConfig().getOptionalValue(configKey, String.class).orElse(defaultValue);
        if (opt == null) {
            if (!allowNull) {
                throw requiredConfigMissing(configKey);
            }
            return null;
        }
        //this is deliberate, we need to make sure the returned value
        //has a unique identity, so if it is passed into the bytecode recorder
        //we know that it is a configured value
        String retVal = new String(opt);
        reverseMap.put(retVal, configKey);
        return retVal;
    }

    public static Set<String> getNames(String prefix) {
        Set<String> props = new HashSet<>();
        for (String i : ConfigProvider.getConfig().getPropertyNames()) {
            if (i.startsWith(prefix)) {
                int idex = i.indexOf('.', prefix.length() + 1);
                if (idex == -1) {
                    props.add(i.substring(prefix.length() + 1));
                } else {
                    props.add(i.substring(prefix.length() + 1, idex));
                }
            }
        }
        return props;
    }

    public static String getConfigKey(Object val) {
        return reverseMap.get(val);
    }

    public static boolean getBoolean(String configKey, String defaultValue) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        if (val == null || val.isEmpty()) {
            return false;
        }
        return asBoolean(val, configKey);
    }

    public static Boolean getBoxedBoolean(String configKey, String defaultValue, boolean allowNull) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        Boolean result;
        if (val == null || val.isEmpty()) {
            if (!allowNull) {
                throw requiredConfigMissing(configKey);
            }
            return null;
        } else {
            result = asBoolean(val, configKey);
        }
        //this is deliberate, we need to make sure the returned value
        //has a unique identity, so if it is passed into the bytecode recorder
        //we know that it is a configured value
        result = new Boolean(result);
        reverseMap.put(result, configKey);
        return result;
    }

    public static int getInt(String configKey, String defaultValue) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        if (val == null || val.isEmpty()) {
            return 0;
        }
        return asInteger(val, configKey);
    }

    public static Integer getBoxedInt(String configKey, String defaultValue, boolean allowNull) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        Integer result;
        if (val == null || val.isEmpty()) {
            if (!allowNull) {
                throw requiredConfigMissing(configKey);
            }
            return null;
        } else {
            result = asInteger(val, configKey);
        }
        //this is deliberate, we need to make sure the returned value
        //has a unique identity, so if it is passed into the bytecode recorder
        //we know that it is a configured value
        result = new Integer(result);
        reverseMap.put(result, configKey);
        return result;
    }

    private static ConfigurationError requiredConfigMissing(final String configKey) {
        return new ConfigurationError("Required configuration property '" + configKey + "' was not defined.");
    }

    private static boolean asBoolean(final String val, final String configKey) {
        final String cleanValue = cleanupValue(val);
        if ("true".equals(cleanValue)) {
            return true;
        } else if ("false".equals(cleanValue)) {
            return false;
        } else {
            throw new ConfigurationError("Configuration value for property '" + configKey + "' was set to '" + val + "'. " +
                    "A boolean value is expected; set this property to either 'true' or 'false'.");
        }
    }

    private static int asInteger(final String val, final String configKey) {
        final String cleanValue = cleanupValue(val);
        try {
            return Integer.parseInt(cleanValue);
        } catch (NumberFormatException nfe) {
            throw new ConfigurationError("Configuration value for property '" + configKey + "' was set to '" + val + "'. " +
                    "An integer value is expected; set this property to a decimal integer.");
        }
    }

    private static String cleanupValue(final String val) {
        return val.trim().toLowerCase(Locale.ROOT);
    }

}
