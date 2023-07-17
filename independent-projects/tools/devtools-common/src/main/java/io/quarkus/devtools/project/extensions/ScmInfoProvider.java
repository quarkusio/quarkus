package io.quarkus.devtools.project.extensions;

import java.util.HashMap;
import java.util.Map;

public class ScmInfoProvider {

    private final String valueFromBuildFile;
    private final String valueFromEnvironment;

    public ScmInfoProvider(String valueFromBuildFile) {
        this.valueFromBuildFile = valueFromBuildFile;
        // We could try and parse the .git/config file, but that will be fragile
        // We could use JGit, something like https://wiki.eclipse.org/JGit/User_Guide#Repository but it seems a lot for our needs
        String repo = System.getenv("GITHUB_REPOSITORY");
        if (repo != null) {
            String qualifiedRepo = "https://github.com/" + repo;
            // Don't try and guess where slashes will be, just deal with any double slashes by brute force
            this.valueFromEnvironment = qualifiedRepo.replace("github.com//", "github.com/");
        } else {
            this.valueFromEnvironment = null;
        }
    }

    public Map<String, String> getSourceRepo() {
        // We use a map here because we might also add things like github tags
        Map<String, String> info = null;
        if (valueFromEnvironment != null) {
            info = new HashMap<>();
            info.put("url", valueFromEnvironment);

        } else if (valueFromBuildFile != null) {
            info = new HashMap<>();
            info.put("url", valueFromBuildFile);
        }

        return info;
    }

    public String getInconsistencyWarning() {
        if (valueFromEnvironment != null && valueFromBuildFile != null && !valueFromEnvironment.equals(valueFromBuildFile)) {
            return "The scm-url coordinates in the build file, " + valueFromBuildFile
                    + " did not match the repository configured in the GITHUB_REPOSITORY environment variable, "
                    + valueFromEnvironment
                    + ". The value which will be used for the extension metadata is "
                    + valueFromEnvironment + ".";
        } else {
            return null;
        }
    }
}
