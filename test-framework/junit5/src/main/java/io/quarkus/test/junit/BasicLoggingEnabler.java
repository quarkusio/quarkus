package io.quarkus.test.junit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * A (global) JUnit callback that enables/sets up basic logging if logging has not already been set up.
 * <p/>
 * This is useful for getting log output from non-Quarkus tests (if executed separately or before the first Quarkus test),
 * but also for getting instant log output from {@code QuarkusTestResourceLifecycleManagers} etc.
 * <p/>
 * This callback can be disabled via {@link #CFGKEY_ENABLED} in {@code junit-platform.properties} or via system property.
 */
public class BasicLoggingEnabler implements BeforeAllCallback {

    private static final String CFGKEY_ENABLED = "junit.quarkus.enable-basic-logging";
    private static Boolean enabled;

    private static final CompletableFuture<Config> configFuture;

    // internal flag, not meant to be used like CFGKEY_ENABLED
    private static final boolean VERBOSE = Boolean.getBoolean(BasicLoggingEnabler.class.getName() + ".verbose");
    private static final long staticInitStart;

    // to speed things up a little, eager async loading of the config that will be looked up in LoggingSetupRecorder
    // downside: doesn't obey CFGKEY_ENABLED, but that should be bearable
    static {
        staticInitStart = VERBOSE ? System.currentTimeMillis() : 0;
        // e.g. continuous testing has everything set up already (DELAYED_HANDLER is active)
        if (!InitialConfigurator.DELAYED_HANDLER.isActivated()
                // at least respect CFGKEY_ENABLED if set as system property
                && Boolean.parseBoolean(System.getProperty(CFGKEY_ENABLED, "true"))) {

            configFuture = CompletableFuture.supplyAsync(BasicLoggingEnabler::buildConfig);
        } else {
            configFuture = CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public synchronized void beforeAll(ExtensionContext context) {
        if (enabled == null) {
            enabled = context.getConfigurationParameter(CFGKEY_ENABLED).map(Boolean::valueOf).orElse(Boolean.TRUE);
        }
        if (!enabled || InitialConfigurator.DELAYED_HANDLER.isActivated()) {
            return;
        }

        var beforeAllStart = VERBOSE ? System.currentTimeMillis() : 0;
        if (VERBOSE) {
            System.out.printf("BasicLoggingEnabler took %s ms from static init to start of beforeAll()%n",
                    beforeAllStart - staticInitStart);
        }

        //////////////////////
        // get the test config

        Config testConfig;
        try {
            testConfig = configFuture.get();
            // highly unlikely, but things might have changed since the static block decided to _not_ load the config
            if (testConfig == null) {
                testConfig = buildConfig();
            }
        } catch (Exception e) {
            // don't be too noisy (don't log the stacktrace)
            System.err.printf("BasicLoggingEnabler failed to retrieve config: %s%n",
                    e instanceof ExecutionException ? ((ExecutionException) e).getCause() : e);
            if (VERBOSE) {
                e.printStackTrace();
            }
            return;
        }

        ///////////////////////////
        // register the test config

        var configProviderResolver = ConfigProviderResolver.instance();
        var tccl = Thread.currentThread().getContextClassLoader();
        Config configToRestore;
        try {
            configProviderResolver.registerConfig(testConfig, tccl);
            configToRestore = null;
        } catch (IllegalStateException e) {
            if (VERBOSE) {
                System.out.println("BasicLoggingEnabler is swapping config after " + e);
            }
            // a config is already registered, which can happen in rare cases,
            // so remember it for later restore, release it and register the test config instead
            configToRestore = configProviderResolver.getConfig();
            configProviderResolver.releaseConfig(configToRestore);
            configProviderResolver.registerConfig(testConfig, tccl);
        }

        ///////////////////
        // activate logging

        try {
            IntegrationTestUtil.activateLogging();
        } catch (RuntimeException e) {
            // don't be too noisy (don't log the stacktrace by default)
            System.err.println("BasicLoggingEnabler failed to enable basic logging: " + e);
            if (VERBOSE) {
                e.printStackTrace();
            }
        } finally {
            // release the config that was registered previously so that tests that try to register their own config
            // don't fail with:
            // "IllegalStateException: SRCFG00017: Configuration already registered for the given class loader"
            // also, a possible recreation of basically the same config for a later test class will consume far less time
            configProviderResolver.releaseConfig(testConfig);
            // if another config was already registered, restore/re-register it now
            if (configToRestore != null) {
                configProviderResolver.registerConfig(configToRestore, tccl);
            }
        }
        if (VERBOSE) {
            System.out.printf("BasicLoggingEnabler took %s ms from start of beforeAll() to end%n",
                    System.currentTimeMillis() - beforeAllStart);
        }
    }

    private static Config buildConfig() {
        // make sure to load ConfigSources with the proper LaunchMode in place
        LaunchMode.set(LaunchMode.TEST);
        // notes:
        // - addDiscovered might seem a bit much, but this ensures that yaml files are loaded (if extension is around)
        // - LaunchMode.NORMAL instead of TEST avoids failing on missing RuntimeOverrideConfigSource$$GeneratedMapHolder
        var start = VERBOSE ? System.currentTimeMillis() : 0;
        var testConfig = ConfigUtils.configBuilder(true, true, LaunchMode.NORMAL).build();
        if (VERBOSE) {
            System.out.printf("BasicLoggingEnabler took %s ms to load config%n", System.currentTimeMillis() - start);
            testConfig.getConfigSources().forEach(s -> System.out.println("BasicLoggingEnabler ConfigSource: " + s));
        }
        return testConfig;
    }
}
