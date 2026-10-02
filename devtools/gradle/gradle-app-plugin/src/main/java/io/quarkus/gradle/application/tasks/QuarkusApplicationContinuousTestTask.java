package io.quarkus.gradle.application.tasks;

import org.gradle.work.DisableCachingByDefault;

/**
 * Runs the standalone application's continuous-testing development session.
 * <p>
 * The plugin registers this task as {@code quarkusApplicationContinuousTest}. It must be invoked with Gradle's
 * {@code --continuous} option and remains active until the continuous build is cancelled. The task is neither
 * configuration-cache compatible nor build-cacheable because it owns a long-lived development-mode process.
 * <p>
 * The supported compatibility contract covers the plugin-registered task and its documented properties and options.
 * No compatibility commitment is made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Gradle-native continuous testing is long-lived and does not produce reusable outputs")
public abstract class QuarkusApplicationContinuousTestTask extends QuarkusApplicationDevTask {
}
