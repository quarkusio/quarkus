package io.quarkus.gradle.tasks;

import org.gradle.api.tasks.TaskAction;

import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class QuarkusListExtensions extends QuarkusTask {

    public QuarkusListExtensions() {
        super("Lists the available quarkus extensions");
    }

    @TaskAction
    public void listExtensions() {
        for (Extension ext : MojoUtils.loadExtensions()) {
            getLogger().lifecycle(
                    ext.getName() + " (" + ext.getGroupId() + ":" + ext.getArtifactId() + ":" + ext.getVersion() + ")");
        }
    }

}
