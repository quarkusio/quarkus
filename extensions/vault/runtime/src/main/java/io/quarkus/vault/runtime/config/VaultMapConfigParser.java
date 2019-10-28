package io.quarkus.vault.runtime.config;

import static java.util.stream.Collectors.toMap;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class VaultMapConfigParser<C> {

    private Pattern pattern;
    private Stream<ConfigSource> configSourceStream;
    private Function<String, C> buildConfigObject;

    public VaultMapConfigParser(Pattern pattern, Function<String, C> buildConfigObject,
            Stream<ConfigSource> configSourceStream) {
        this.pattern = pattern;
        this.configSourceStream = configSourceStream;
        this.buildConfigObject = buildConfigObject;
    }

    public Map<String, C> getConfig() {

        return configSourceStream
                .flatMap(configSource -> configSource.getPropertyNames().stream())
                .map(this::getKey)
                .filter(Objects::nonNull)
                .distinct()
                .map(this::createKeyValuePair)
                .collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private AbstractMap.SimpleEntry<String, C> createKeyValuePair(String name) {
        return new AbstractMap.SimpleEntry<>(name, buildConfigObject.apply(name));
    }

    private String getKey(String propertyName) {
        Matcher matcher = pattern.matcher(propertyName);
        return matcher.find() ? matcher.group(1) : null;
    }

}
