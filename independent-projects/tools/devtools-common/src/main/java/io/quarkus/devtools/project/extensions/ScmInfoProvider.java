package io.quarkus.devtools.project.extensions;

import java.util.HashMap;
import java.util.Map;

public class ScmInfoProvider {

    public static Map<String, String> getSourceRepo(String valueFromBuildFile) {
        // We could try and parse the .git/config file, but that will be fragile
        // We could use JGit, something like https://wiki.eclipse.org/JGit/User_Guide#Repository but it seems a lot for our needs
        String repo = System.getenv("GITHUB_REPOSITORY");
        Map info = null;
        if (repo != null) {
            info = new HashMap();
            String qualifiedRepo = "https://github.com/" + repo;
            // Don't try and guess where slashes will be, just deal with any double slashes by brute force
            qualifiedRepo = qualifiedRepo.replace("github.com//", "github.com/");
            info.put("url", qualifiedRepo);

        } else if (valueFromBuildFile != null) {
            info = new HashMap();
            info.put("url", valueFromBuildFile);
        }
        return info;
    }

}
