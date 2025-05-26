package io.quarkus.deployment.pkg;

import java.io.File;
import java.util.List;
import java.util.Optional;

import io.quarkus.deployment.util.ContainerRuntimeUtil;

public class TestNativeConfig implements NativeConfig {

    private final NativeConfig.BuilderImageConfig builderImage;

    public TestNativeConfig(String builderImage) {
        this(builderImage, ImagePullStrategy.ALWAYS);
    }

    public TestNativeConfig(ImagePullStrategy builderImagePull) {
        this("mandrel", builderImagePull);
    }

    public TestNativeConfig(String builderImage, ImagePullStrategy builderImagePull) {
        this.builderImage = new TestBuildImageConfig(builderImage, builderImagePull);
    }

    public boolean enabled() {
        return true;
    }

    public boolean sourcesOnly() {
        return true;
    }

    @Override
    public Optional<List<String>> additionalBuildArgs() {
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> additionalBuildArgsAppend() {
        return Optional.empty();
    }

    @Override
    public boolean enableHttpUrlHandler() {
        return false;
    }

    @Override
    public boolean enableHttpsUrlHandler() {
        return false;
    }

    @Override
    public boolean enableAllSecurityServices() {
        return false;
    }

    @Override
    public boolean inlineBeforeAnalysis() {
        return false;
    }

    @Override
    public boolean enableJni() {
        return false;
    }

    @Override
    public boolean headless() {
        return false;
    }

    @Override
    public Optional<String> userLanguage() {
        return Optional.empty();
    }

    @Override
    public Optional<String> userCountry() {
        return Optional.empty();
    }

    @Override
    public String fileEncoding() {
        return null;
    }

    @Override
    public boolean addAllCharsets() {
        return false;
    }

    @Override
    public Optional<String> graalvmHome() {
        return Optional.empty();
    }

    @Override
    public File javaHome() {
        return null;
    }

    @Override
    public Optional<String> nativeImageXmx() {
        return Optional.empty();
    }

    @Override
    public boolean debugBuildProcess() {
        return false;
    }

    @Override
    public boolean publishDebugBuildProcessPort() {
        return false;
    }

    @Override
    public boolean cleanupServer() {
        return false;
    }

    @Override
    public boolean enableIsolates() {
        return false;
    }

    @Override
    public boolean enableFallbackImages() {
        return false;
    }

    @Override
    public boolean enableServer() {
        return false;
    }

    @Override
    public boolean autoServiceLoaderRegistration() {
        return false;
    }

    @Override
    public boolean dumpProxies() {
        return false;
    }

    @Override
    public Optional<Boolean> containerBuild() {
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> pie() {
        return Optional.empty();
    }

    @Override
    public Optional<String> march() {
        return Optional.empty();
    }

    @Override
    public boolean remoteContainerBuild() {
        return false;
    }

    @Override
    public BuilderImageConfig builderImage() {
        return builderImage;
    }

    @Override
    public Optional<ContainerRuntimeUtil.ContainerRuntime> containerRuntime() {
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> containerRuntimeOptions() {
        return Optional.empty();
    }

    @Override
    public boolean enableVmInspection() {
        return false;
    }

    @Override
    public Optional<List<MonitoringOption>> monitoring() {
        return Optional.empty();
    }

    @Override
    public boolean fullStackTraces() {
        return false;
    }

    @Override
    public boolean enableReports() {
        return false;
    }

    @Override
    public boolean reportExceptionStackTraces() {
        return false;
    }

    @Override
    public boolean reportErrorsAtRuntime() {
        return false;
    }

    @Override
    public boolean reuseExisting() {
        return false;
    }

    @Override
    public ResourcesConfig resources() {
        return null;
    }

    @Override
    public Debug debug() {
        return null;
    }

    @Override
    public boolean enableDashboardDump() {
        return false;
    }

    @Override
    public boolean includeReasonsInConfigFiles() {
        return false;
    }

    @Override
    public Compression compression() {
        return null;
    }

    @Override
    public boolean agentConfigurationApply() {
        return false;
    }

    private class TestBuildImageConfig implements BuilderImageConfig {
        private final String image;
        private final ImagePullStrategy pull;

        TestBuildImageConfig(String image, ImagePullStrategy pull) {
            this.image = image;
            this.pull = pull;
        }

        @Override
        public String image() {
            return image;
        }

        @Override
        public ImagePullStrategy pull() {
            return pull;
        }
    }
}
