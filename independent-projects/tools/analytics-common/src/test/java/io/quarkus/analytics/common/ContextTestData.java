package io.quarkus.analytics.common;

import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_APP;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_BUILD;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_GRAALVM;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_GRADLE_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_IP;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_JAVA;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_JAVA_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_LOCALE_COUNTRY;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_LOCATION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_MAVEN_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_NAME;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_OS;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_OS_ARCH;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_QUARKUS;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_TIMEZONE;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_VENDOR;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.VALUE_NULL_IP;

import java.util.Map;

import io.quarkus.analytics.dto.segment.ContextBuilder;

public class ContextTestData {
    public static Map<String, Object> createContext() {
        return new ContextBuilder()
                .mapPair(PROP_APP)
                .pair(PROP_NAME, "app-name")
                .build()
                .mapPair(PROP_JAVA)
                .pair(PROP_VENDOR, "Eclipse")
                .pair(PROP_VERSION, "17")
                .build()
                .mapPair(PROP_GRAALVM)
                .pair(PROP_VENDOR, "N/A")
                .pair(PROP_VERSION, "N/A")
                .pair(PROP_JAVA_VERSION, "N/A")
                .build()
                .mapPair(PROP_BUILD)
                .pair(PROP_MAVEN_VERSION, "3.8,1")
                .pair(PROP_GRADLE_VERSION, "N/A")
                .build()
                .mapPair(PROP_QUARKUS)
                .pair(PROP_VERSION, "N/A")
                .build()
                .pair(PROP_IP, VALUE_NULL_IP)
                .mapPair(PROP_LOCATION)
                .pair(PROP_LOCALE_COUNTRY, "Portugal")
                .build()
                .mapPair(PROP_OS)
                .pair(PROP_NAME, "arm64")
                .pair(PROP_VERSION, "1234")
                .pair(PROP_OS_ARCH, "MacOs")
                .build()
                .pair(PROP_TIMEZONE, "Europe/Lisbon")
                .build();
    }
}
