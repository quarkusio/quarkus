package io.quarkus.bootstrap.devmode;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependency;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

public class DependenciesFilter {

    private static final Logger log = Logger.getLogger(DependenciesFilter.class);

    public static List<ResolvedDependency> getReloadableModules(ApplicationModel appModel) {
        final List<ResolvedDependency> reloadable = new ArrayList<>();
        if (appModel.getApplicationModule() != null) {
            reloadable.add(appModel.getAppArtifact());
        }
        appModel.getDependencies().forEach(d -> {
            if (d.isReloadable()) {
                reloadable.add(d);
            } else if (d.isWorkspaceModule()) {
                //if this project also contains Quarkus extensions we do no want to include these in the discovery
                //a bit of an edge case, but if you try and include a sample project with your extension you will
                //run into problems without this
                final StringBuilder msg = new StringBuilder();
                msg.append("Local Quarkus extension dependency ");
                msg.append(d.getGroupId()).append(":").append(d.getArtifactId()).append(":");
                if (!d.getClassifier().isEmpty()) {
                    msg.append(d.getClassifier()).append(":");
                }
                if (!GACTV.TYPE_JAR.equals(d.getType())) {
                    msg.append(d.getType()).append(":");
                }
                msg.append(d.getVersion()).append(" will not be hot-reloadable");
                log.warn(msg.toString());
            }
        });
        return reloadable;
    }
}
