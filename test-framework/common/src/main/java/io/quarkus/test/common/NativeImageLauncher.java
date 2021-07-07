package io.quarkus.test.common;

public interface NativeImageLauncher extends ArtifactLauncher<NativeImageLauncher.NativeImageInitContext> {

    interface NativeImageInitContext extends InitContext {

        String nativeImagePath();

        Class<?> testClass();
    }
}
