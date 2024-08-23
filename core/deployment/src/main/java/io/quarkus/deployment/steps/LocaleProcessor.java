package io.quarkus.deployment.steps;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.runtime.LocalesBuildTimeConfig;

/**
 * In order for a Native image built app to be able to use localized names of e.g. countries,
 * these language bundles have to be loaded. JDK uses ServiceLoader approach for that.
 * For instance, Locale.FRANCE.getDisplayCountry(Locale.GERMAN) must print "Frankreich".
 */
public class LocaleProcessor {

    private static final Logger log = Logger.getLogger(LocaleProcessor.class);
    public static final String DEPRECATED_USER_LANGUAGE_WARNING = "Your application is setting the deprecated 'quarkus.native.user-language' configuration property. "
            +
            "Please, consider using only 'quarkus.default-locale' configuration property instead.";
    public static final String DEPRECATED_USER_COUNTRY_WARNING = "Your application is setting the deprecated 'quarkus.native.user-country' configuration property. "
            +
            "Please, consider using only 'quarkus.default-locale' configuration property instead.";

    @BuildStep(onlyIf = { NativeBuild.class, NonDefaultLocale.class })
    void nativeResources(BuildProducer<NativeImageResourceBundleBuildItem> resources) {
        resources.produce(new NativeImageResourceBundleBuildItem("sun.util.resources.LocaleNames", "java.base"));
        resources.produce(new NativeImageResourceBundleBuildItem("sun.util.resources.CurrencyNames", "java.base"));
        //Adding sun.util.resources.TimeZoneNames is not necessary.
    }

    @BuildStep(onlyIf = { NativeBuild.class, NonDefaultLocale.class })
    ReflectiveClassBuildItem setupReflectionClasses() {
        return ReflectiveClassBuildItem.builder("sun.util.resources.provider.SupplementaryLocaleDataProvider",
                "sun.util.resources.provider.LocaleDataProvider").build();
    }

    @BuildStep(onlyIf = { NativeBuild.class, NonDefaultLocale.class })
    void servicesResource(BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {
        final String r1 = "META-INF/services/sun.util.resources.LocaleData$SupplementaryResourceBundleProvider";
        final String r2 = "META-INF/services/sun.util.resources.LocaleData$CommonResourceBundleProvider";
        nativeImageResources.produce(new NativeImageResourceBuildItem(r1, r2));
        generatedResources.produce(new GeneratedResourceBuildItem(r1,
                "sun.util.resources.provider.SupplementaryLocaleDataProvider".getBytes(StandardCharsets.UTF_8)));
        generatedResources.produce(new GeneratedResourceBuildItem(r2,
                "sun.util.resources.provider.LocaleDataProvider".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * We activate additional resources in native-image executable only if user opts
     * for anything else than what is already the system default.
     */
    static final class NonDefaultLocale implements BooleanSupplier {
        private final NativeConfig nativeConfig;
        private final LocalesBuildTimeConfig localesBuildTimeConfig;

        public NonDefaultLocale(NativeConfig nativeConfig, LocalesBuildTimeConfig localesBuildTimeConfig) {
            this.nativeConfig = nativeConfig;
            this.localesBuildTimeConfig = localesBuildTimeConfig;
        }

        @Override
        public boolean getAsBoolean() {
            return (nativeConfig.userLanguage().isPresent()
                    && !Locale.getDefault().getLanguage().equals(nativeConfig.userLanguage().get()))
                    ||
                    (nativeConfig.userCountry().isPresent()
                            && !Locale.getDefault().getCountry().equals(nativeConfig.userCountry().get()))
                    ||
                    !Locale.getDefault().equals(localesBuildTimeConfig.defaultLocale)
                    ||
                    localesBuildTimeConfig.locales.stream().anyMatch(l -> !Locale.getDefault().equals(l));
        }
    }

    /**
     * User language for native-image executable.
     *
     * @param nativeConfig
     * @param localesBuildTimeConfig
     * @return User language set by 'quarkus.default-locale' or by deprecated 'quarkus.native.user-language' or
     *         effectively LocalesBuildTimeConfig.DEFAULT_LANGUAGE if none of the aforementioned is set.
     */
    public static String nativeImageUserLanguage(NativeConfig nativeConfig, LocalesBuildTimeConfig localesBuildTimeConfig) {
        String language = localesBuildTimeConfig.defaultLocale.getLanguage();
        if (nativeConfig.userLanguage().isPresent()) {
            log.warn(DEPRECATED_USER_LANGUAGE_WARNING);
            // The deprecated option takes precedence for users who are already using it.
            language = nativeConfig.userLanguage().get();
        }
        return language;
    }

    /**
     * User country for native-image executable.
     *
     * @param nativeConfig
     * @param localesBuildTimeConfig
     * @return User country set by 'quarkus.default-locale' or by deprecated 'quarkus.native.user-country' or
     *         effectively LocalesBuildTimeConfig.DEFAULT_COUNTRY (could be an empty string) if none of the aforementioned is
     *         set.
     */
    public static String nativeImageUserCountry(NativeConfig nativeConfig, LocalesBuildTimeConfig localesBuildTimeConfig) {
        String country = localesBuildTimeConfig.defaultLocale.getCountry();
        if (nativeConfig.userCountry().isPresent()) {
            log.warn(DEPRECATED_USER_COUNTRY_WARNING);
            // The deprecated option takes precedence for users who are already using it.
            country = nativeConfig.userCountry().get();
        }
        return country;
    }

    /**
     * Additional locales to be included in native-image executable.
     *
     * @param nativeConfig
     * @param localesBuildTimeConfig
     * @return A comma separated list of IETF BCP 47 language tags, optionally with ISO 3166-1 alpha-2 country codes.
     *         As a special case a string "all" making the native-image to include all available locales.
     */
    public static String nativeImageIncludeLocales(NativeConfig nativeConfig, LocalesBuildTimeConfig localesBuildTimeConfig) {
        // We start with what user sets as needed locales
        final Set<Locale> additionalLocales = new HashSet<>(localesBuildTimeConfig.locales);

        if (additionalLocales.contains(Locale.ROOT)) {
            return "all";
        }

        // We subtract what we already declare for native-image's user.language or user.country.
        // Note the deprecated options still count.
        additionalLocales.remove(localesBuildTimeConfig.defaultLocale);
        if (nativeConfig.userCountry().isPresent() && nativeConfig.userLanguage().isPresent()) {
            additionalLocales.remove(new Locale(nativeConfig.userLanguage().get(), nativeConfig.userCountry().get()));
        } else if (nativeConfig.userLanguage().isPresent()) {
            additionalLocales.remove(new Locale(nativeConfig.userLanguage().get()));
        }

        return additionalLocales.stream()
                .filter(l -> !Locale.getDefault().equals(l))
                .map(l -> l.getLanguage() + (l.getCountry().isEmpty() ? "" : "-" + l.getCountry()))
                .collect(Collectors.joining(","));
    }

}
