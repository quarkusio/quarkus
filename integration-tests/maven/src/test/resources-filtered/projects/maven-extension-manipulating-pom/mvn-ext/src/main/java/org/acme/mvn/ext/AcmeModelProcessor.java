package org.acme.mvn.ext;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelProcessor;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

@Component(role = ModelProcessor.class)
public class AcmeModelProcessor extends DefaultModelProcessor {

    private static final String ACME_VERSION = "1.0.0-acme-00001";
    private static final String PROJECT_VERSION = "\${project.version}";

    @Override
    public Model read(File input, Map<String, ?> options) throws IOException {
        return provisionModel(super.read(input, options), options);
    }

    @Override
    public Model read(Reader input, Map<String, ?> options) throws IOException {
        return provisionModel(super.read(input, options), options);
    }

    @Override
    public Model read(InputStream input, Map<String, ?> options) throws IOException {
        return provisionModel(super.read(input, options), options);
    }

    private Model provisionModel(Model model, Map<String, ?> options) {
        if("org.acme".equals(getGroupId(model))) {
            setAcmeVersion(model, ACME_VERSION);
        }
        return model;
    }

    private static void setAcmeVersion(Model model, String version) {
        if(model.getVersion() != null) {
            model.setVersion(version);
        }
        resolveProjectVersionVariable(version, model);

        if(model.getParent() != null) {
            model.getParent().setVersion(version);
        }
    }

    private static void resolveProjectVersionVariable(String version, Model model) {
        // resolve project.version in properties
        if (model.getProperties() != null) {
            for (Map.Entry<Object, Object> entry : model.getProperties().entrySet()) {
                if (PROJECT_VERSION.equals(entry.getValue())) {
                    entry.setValue(version);
                }
            }
        }

        // resolve project.version in dependencies
        if (model.getDependencies() != null) {
            for (Dependency dependency : model.getDependencies()) {
                if (PROJECT_VERSION.equals(dependency.getVersion())) {
                    dependency.setVersion(version);
                }
            }
        }

        // resole project.version in dependencyManagement
        if (model.getDependencyManagement() != null
                && model.getDependencyManagement().getDependencies() != null) {
            for (Dependency dependency : model.getDependencyManagement().getDependencies()) {
                if (PROJECT_VERSION.equals(dependency.getVersion())) {
                    dependency.setVersion(version);
                }
            }
        }

        // resolve project.version in plugins
        if (model.getBuild() != null && model.getBuild().getPlugins() != null) {
            for (Plugin plugin : model.getBuild().getPlugins()) {
                if (plugin.getDependencies() != null) {
                    for (Dependency dependency : plugin.getDependencies()) {
                        if (PROJECT_VERSION.equals(dependency.getVersion())) {
                            dependency.setVersion(version);
                        }
                    }
                }
            }
        }

        // resolve project.version in pluginManagement
        if (model.getBuild() != null
                && model.getBuild().getPluginManagement() != null
                && model.getBuild().getPluginManagement().getPlugins() != null) {
            for (Plugin plugin : model.getBuild().getPluginManagement().getPlugins()) {
                if (plugin.getDependencies() != null) {
                    for (Dependency dependency : plugin.getDependencies()) {
                        if (PROJECT_VERSION.equals(dependency.getVersion())) {
                            dependency.setVersion(version);
                        }
                    }
                }
            }
        }
    }

    private static String getGroupId(Model model) {
        if(model.getGroupId() != null) {
            return model.getGroupId();
        }
        return model.getParent() == null ? null : model.getParent().getGroupId();
    }
}
