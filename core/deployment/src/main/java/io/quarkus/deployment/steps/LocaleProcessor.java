package io.quarkus.deployment.steps;

import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.steps.NativeBuild;

/**
 * In order for a Native image built app to be able to use localized names of e.g. countries,
 * these language bundles have to be loaded. JDK uses ServiceLoader approach for that.
 * For instance, Locale.FRANCE.getDisplayCountry(Locale.GERMAN) must print "Frankreich".
 */
public class LocaleProcessor {

    @BuildStep(onlyIf = { NativeBuild.class, NonEnglishLocale.class })
    void nativeResources(BuildProducer<NativeImageResourceBundleBuildItem> resources) {
        resources.produce(new NativeImageResourceBundleBuildItem("sun.util.resources.LocaleNames"));
        resources.produce(new NativeImageResourceBundleBuildItem("sun.util.resources.CurrencyNames"));
        //Adding sun.util.resources.TimeZoneNames is not necessary.
    }

    @BuildStep(onlyIf = { NativeBuild.class, NonEnglishLocale.class })
    ReflectiveClassBuildItem setupReflectionClasses() {
        return new ReflectiveClassBuildItem(false, false,
                "sun.util.resources.provider.SupplementaryLocaleDataProvider",
                "sun.util.resources.provider.LocaleDataProvider");
    }

    @BuildStep(onlyIf = { NativeBuild.class, NonEnglishLocale.class })
    void servicesResource(BuildProducer<NativeImageResourceBuildItem> nibProducer,
            BuildProducer<GeneratedResourceBuildItem> genResProducer) {
        final String r1 = "META-INF/services/sun.util.resources.LocaleData$SupplementaryResourceBundleProvider";
        final String r2 = "META-INF/services/sun.util.resources.LocaleData$CommonResourceBundleProvider";
        nibProducer.produce(new NativeImageResourceBuildItem(r1, r2));
        genResProducer.produce(new GeneratedResourceBuildItem(r1,
                "sun.util.resources.provider.SupplementaryLocaleDataProvider".getBytes(StandardCharsets.UTF_8)));
        genResProducer.produce(new GeneratedResourceBuildItem(r2,
                "sun.util.resources.provider.LocaleDataProvider".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * We activate additional resources in native-image executable only if user opts
     * for something else than US English.
     */
    static final class NonEnglishLocale implements BooleanSupplier {
        private static final Pattern US_ENGLISH = Pattern.compile("(?i:(en|,|-US)+)");
        private final NativeConfig nativeConfig;

        public NonEnglishLocale(NativeConfig nativeConfig) {
            this.nativeConfig = nativeConfig;
        }

        @Override
        public boolean getAsBoolean() {
            return (nativeConfig.userLanguage.isPresent() && !US_ENGLISH.matcher(nativeConfig.userLanguage.get()).matches())
                    ||
                    (nativeConfig.userCountry.isPresent() && !US_ENGLISH.matcher(nativeConfig.userCountry.get()).matches())
                    ||
                    (nativeConfig.locales.isPresent() && !US_ENGLISH.matcher(nativeConfig.locales.get()).matches());
        }
    }
}
