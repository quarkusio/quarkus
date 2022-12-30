package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder.SmallRyeReactiveMessagingContext;

public class EnvironmentVariableRedefiningConfigSource implements ConfigSource {
    private static final String INCOMING_PREFIX = "MP_MESSAGING_INCOMING_";

    private static final Pattern P_NON_ALPHANUM = Pattern.compile("[^a-zA-Z0-9]");

    private static final String OUTGOING_PREFIX = "MP_MESSAGING_OUTGOING_";

    private Set<String> propertyNames;

    @Override
    public Map<String, String> getProperties() {
        return ConfigSource.super.getProperties();
    }

    @Override
    public int getOrdinal() {
        return Integer.MIN_VALUE;
    }

    @Override
    public Set<String> getPropertyNames() {
        if (propertyNames == null && Arc.container() != null) {
            propertyNames = new HashSet<>();
            try (InstanceHandle<SmallRyeReactiveMessagingContext> contextInstance = Arc.container()
                    .instance(SmallRyeReactiveMessagingContext.class)) {
                Map<String, List<String>> prefixes = buildPrefixMap(contextInstance.get());

                for (Map.Entry<String, String> envVarEntry : System.getenv().entrySet()) {
                    for (Map.Entry<String, List<String>> channelPrefixesEntry : prefixes.entrySet()) {
                        String matchingPrefix = findMatchingPrefix(envVarEntry.getKey(), channelPrefixesEntry.getValue());
                        if (matchingPrefix != null) {
                            String prefix = matchingPrefix
                                    .substring(0, matchingPrefix.length() - channelPrefixesEntry.getKey().length());
                            String suffix = envVarEntry.getKey().substring(matchingPrefix.length());
                            propertyNames
                                    .add(toDottedLowerCase(prefix) + channelPrefixesEntry.getKey() + toDottedLowerCase(suffix));
                            break;
                        }
                    }
                }
            }
        }
        return propertyNames == null ? Collections.emptySet() : propertyNames;
    }

    private static Map<String, List<String>> buildPrefixMap(SmallRyeReactiveMessagingContext context) {
        Map<String, List<String>> prefixes = new HashMap<>();
        Set<String> channels = new HashSet<>();
        context.getChannelConfigurations().forEach(config -> channels.add(config.channelName));
        context.getMediatorConfigurations().forEach(config -> {
            channels.addAll(config.getIncoming());
            if (config.getOutgoing() != null) {
                channels.add(config.getOutgoing());
            }
        });
        for (String channelName : channels) {
            String uppercasedName = P_NON_ALPHANUM.matcher(channelName.toUpperCase()).replaceAll("_");
            prefixes.put(channelName, List.of(INCOMING_PREFIX + uppercasedName, OUTGOING_PREFIX + uppercasedName));
        }
        return prefixes;
    }

    private static String toDottedLowerCase(String text) {
        return text.toLowerCase().replaceAll("_", ".");
    }

    private static String findMatchingPrefix(String toTest, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (toTest.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    @Override
    public String getValue(String s) {
        return null;
    }

    @Override
    public String getName() {
        return EnvironmentVariableRedefiningConfigSource.class.getSimpleName();
    }
}
