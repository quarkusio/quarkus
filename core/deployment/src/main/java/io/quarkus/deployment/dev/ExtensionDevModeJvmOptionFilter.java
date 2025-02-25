package io.quarkus.deployment.dev;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.maven.dependency.ArtifactCoordsPattern;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Extension Dev mode JVM argument filter configuration
 */
public class ExtensionDevModeJvmOptionFilter {

    private boolean disableAll;

    private List<String> disableFor = List.of();
    private List<ArtifactCoordsPattern> disableForPatterns;

    public boolean isDisableAll() {
        return disableAll;
    }

    public void setDisableAll(boolean disableAll) {
        this.disableAll = disableAll;
        resetPatterns();
    }

    public List<String> getDisableFor() {
        return disableFor;
    }

    public void setDisableFor(List<String> disableFor) {
        this.disableFor = disableFor;
        resetPatterns();
    }

    private void resetPatterns() {
        disableForPatterns = null;
    }

    List<ArtifactCoordsPattern> getDisableForPatterns() {
        if (disableFor.isEmpty()) {
            return List.of();
        }
        if (disableForPatterns == null) {
            var result = new ArrayList<ArtifactCoordsPattern>(disableFor.size());
            for (var s : disableFor) {
                result.add(ArtifactCoordsPattern.of(s));
            }
            disableForPatterns = result;
        }
        return disableForPatterns;
    }

    boolean isDisabled(ArtifactKey extensionKey) {
        for (var pattern : getDisableForPatterns()) {
            if (pattern.matches(extensionKey.getGroupId(), extensionKey.getArtifactId(), extensionKey.getClassifier(), "jar",
                    null)) {
                return true;
            }
        }
        return false;
    }

}
