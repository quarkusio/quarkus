package io.quarkus.bootstrap.devmode;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

public class DependenciesFilter {

    private static final Logger log = Logger.getLogger(DependenciesFilter.class);

    public static List<WorkspaceModule> getReloadableModules(ApplicationModel appModel) {
        final List<WorkspaceModule> reloadable = new ArrayList<>();
        if (appModel.getApplicationModule() != null) {
            reloadable.add(appModel.getApplicationModule());
        }
        appModel.getDependencies().forEach(d -> {
            final WorkspaceModule module = d.getWorkspaceModule();
            if (module != null) {
                if (d.isReloadable()) {
                    reloadable.add(module);
                } else {
                    //if this project also contains Quarkus extensions we do no want to include these in the discovery
                    //a bit of an edge case, but if you try and include a sample project with your extension you will
                    //run into problems without this
                    log.warn("Local Quarkus extension dependency " + module.getId() + " will not be hot-reloadable");
                }
            }
        });
        return reloadable;
    }
}
