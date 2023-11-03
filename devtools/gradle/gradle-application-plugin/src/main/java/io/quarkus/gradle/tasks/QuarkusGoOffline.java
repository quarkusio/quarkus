package io.quarkus.gradle.tasks;

import javax.inject.Inject;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.runtime.LaunchMode;

public abstract class QuarkusGoOffline extends QuarkusTask {

    private Configuration compileClasspath;
    private Configuration testCompileClasspath;
    private Configuration quarkusDevClasspath;

    @Inject
    public QuarkusGoOffline() {
        super("Resolve all dependencies for offline usage");
    }

    @CompileClasspath
    public Configuration getCompileClasspath() {
        return compileClasspath;
    }

    public void setCompileClasspath(Configuration compileClasspath) {
        this.compileClasspath = compileClasspath;
    }

    @CompileClasspath
    public Configuration getTestCompileClasspath() {
        return testCompileClasspath;
    }

    public void setTestCompileClasspath(Configuration testCompileClasspath) {
        this.testCompileClasspath = testCompileClasspath;
    }

    @CompileClasspath
    public Configuration getQuarkusDevClasspath() {
        return quarkusDevClasspath;
    }

    public void setQuarkusDevClasspath(Configuration quarkusDevClasspath) {
        this.quarkusDevClasspath = quarkusDevClasspath;
    }

    @TaskAction
    public void resolveAllModels() {
        extension().getApplicationModel(LaunchMode.NORMAL);
        extension().getApplicationModel(LaunchMode.DEVELOPMENT);
        extension().getApplicationModel(LaunchMode.TEST);
    }

}
