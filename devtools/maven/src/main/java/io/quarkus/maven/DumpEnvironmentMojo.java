package io.quarkus.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "dump-env")
public class DumpEnvironmentMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("ENVIRONMENT");
        List<String> keys = new ArrayList<>(System.getenv().keySet());
        Collections.sort(keys);
        for (String key : keys) {
            getLog().info(key + "=" + System.getenv(key));
        }

        getLog().info("------------");
        getLog().info("ENVIRONMENT");
        keys.clear();
        for (Object key : System.getProperties().keySet()) {
            keys.add(key.toString());
        }
        Collections.sort(keys);
        for (String key : keys) {
            getLog().info(key + "=" + System.getProperty(key));
        }
    }
}
