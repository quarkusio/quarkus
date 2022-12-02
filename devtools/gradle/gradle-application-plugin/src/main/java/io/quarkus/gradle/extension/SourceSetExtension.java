package io.quarkus.gradle.extension;

import org.gradle.api.tasks.SourceSet;

public class SourceSetExtension {

    private SourceSet extraNativeTestSourceSet;

    public SourceSet extraNativeTest() {
        return extraNativeTestSourceSet;
    }

    public void setExtraNativeTest(SourceSet extraNativeTestSourceSet) {
        this.extraNativeTestSourceSet = extraNativeTestSourceSet;
    }
}
