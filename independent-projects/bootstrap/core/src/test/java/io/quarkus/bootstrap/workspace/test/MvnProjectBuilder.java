/**
 *
 */
package io.quarkus.bootstrap.workspace.test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;

public class MvnProjectBuilder {

    public static final String DEFAULT_GROUP_ID = "io.quarkus.test";
    public static final String DEFAULT_VERSION = "1.0";

    private MvnProjectBuilder parent;
    private Model model = new Model();
    private List<MvnProjectBuilder> modules = new ArrayList<>(0);

    public static MvnProjectBuilder forArtifact(String artifactId) {
        return new MvnProjectBuilder(artifactId);
    }

    private MvnProjectBuilder(String artifactId) {
        this(artifactId, null);
    }

    private MvnProjectBuilder(String artifactId, MvnProjectBuilder parent) {
        model.setModelVersion("4.0.0");
        model.setGroupId(DEFAULT_GROUP_ID);
        model.setArtifactId(artifactId);
        model.setVersion(DEFAULT_VERSION);
        model.setPackaging("jar");
        this.parent = parent;
    }

    public MvnProjectBuilder getParent() {
        return parent;
    }

    public MvnProjectBuilder setParent(Parent parent) {
        model.setParent(parent);
        return this;
    }

    public MvnProjectBuilder setGroupId(String groupId) {
        model.setGroupId(groupId);
        return this;
    }

    public MvnProjectBuilder setVersion(String version) {
        model.setVersion(version);
        return this;
    }

    public MvnProjectBuilder addDependency(String artifactId) {
        final Dependency dep = new Dependency();
        dep.setGroupId(DEFAULT_GROUP_ID);
        dep.setArtifactId(artifactId);
        dep.setVersion(DEFAULT_VERSION);
        return addDependency(dep);
    }

    public MvnProjectBuilder addDependency(String artifactId, String scope) {
        final Dependency dep = new Dependency();
        dep.setGroupId(DEFAULT_GROUP_ID);
        dep.setArtifactId(artifactId);
        dep.setVersion(DEFAULT_VERSION);
        dep.setScope(scope);
        return addDependency(dep);
    }

    public MvnProjectBuilder addDependency(Dependency dep) {
        model.addDependency(dep);
        return this;
    }

    public MvnProjectBuilder addModule(String path, String artifactId) {
        return addModule(path, artifactId, true);
    }

    public MvnProjectBuilder addModule(String path, String artifactId, boolean initParent) {
        model.addModule(path);
        final MvnProjectBuilder module = new MvnProjectBuilder(artifactId, this);
        if(initParent) {
            final Parent parentModel = new Parent();
            module.model.setParent(parentModel);
            parentModel.setGroupId(model.getGroupId());
            parentModel.setArtifactId(model.getArtifactId());
            parentModel.setVersion(model.getVersion());
            final Path rootDir = Paths.get("").toAbsolutePath();
            final Path moduleDir = rootDir.resolve(path).normalize();
            if(!moduleDir.getParent().equals(rootDir)) {
                parentModel.setRelativePath(moduleDir.relativize(rootDir).toString());
            }
        }
        modules.add(module);
        return module;
    }

    public void build(Path projectDir) {
        //System.out.println("build " + model.getArtifactId() + " " + projectDir);
        IoUtils.mkdirs(projectDir);
        if(!modules.isEmpty()) {
            model.setPackaging("pom");
            for(int i = 0; i < modules.size(); ++i) {
                modules.get(i).build(projectDir.resolve(model.getModules().get(i)));
            }
        } else {
            IoUtils.mkdirs(projectDir.resolve("target").resolve("classes"));
        }
        try {
            ModelUtils.persistModel(projectDir.resolve("pom.xml"), model);
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }
}
