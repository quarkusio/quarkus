package io.quarkus.gradle.application.tasks;

import java.util.Map;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.config.EffectiveConfigDiagnosticsFormatter;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;

/**
 * Reports the effective configuration used by a named application build without executing that build.
 * Values are hidden by default. The {@code --show-values} task option reveals captured non-ambient values and must not be
 * used in shared logs. Values won by system-property and environment configuration sources are always omitted.
 * The task always executes because its result is diagnostic log output rather than a reusable file.
 * <p>
 * The supported compatibility contract covers plugin-registered instances and the documented task names, properties,
 * and options. No compatibility commitment is made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Effective-configuration diagnostics only log resolved configuration")
public abstract class QuarkusApplicationShowEffectiveConfigTask extends QuarkusApplicationEffectiveConfigTask {

    private boolean showValues;

    /**
     * Creates a diagnostic task with no operation-forced properties that always produces log output.
     */
    public QuarkusApplicationShowEffectiveConfigTask() {
        getBuildOperationForcedProperties().convention(Map.of());
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Returns Quarkus properties forced by the operation whose effective configuration is being reported.
     * This is plugin wiring rather than an independent user configuration surface.
     *
     * @return the operation-forced properties
     */
    @Input
    public abstract MapProperty<String, String> getBuildOperationForcedProperties();

    /**
     * Controls whether captured non-ambient configuration values are printed.
     * <p>
     * The default is {@code false}. Enabling the option may expose credentials in retained local or CI logs.
     * System-property and environment-variable winners remain omitted.
     *
     * @param showValues whether to print captured non-ambient values
     */
    @Option(option = "show-values", description = "Show captured non-ambient values; output may contain credentials and be retained in build or CI logs")
    public void setShowValues(boolean showValues) {
        this.showValues = showValues;
    }

    /**
     * Logs the effective configuration plan, warning first when values or legacy ambient capture are enabled.
     */
    @TaskAction
    public void showEffectiveConfig() {
        warnIfLegacyAmbientConfigCaptureEnabled();
        if (showValues) {
            getLogger().warn("Showing captured effective configuration values. "
                    + "They may contain credentials and may be retained in build or CI logs.");
        }
        getLogger().lifecycle("{}", diagnostics());
    }

    String diagnostics() {
        EffectiveConfigPlan plan = effectiveConfig(getBuildOperationForcedProperties().get());
        return EffectiveConfigDiagnosticsFormatter.format(
                getPath(), getBuildName().get(), getBuildType().get(), effectiveConfigProfile(), plan, showValues);
    }
}
