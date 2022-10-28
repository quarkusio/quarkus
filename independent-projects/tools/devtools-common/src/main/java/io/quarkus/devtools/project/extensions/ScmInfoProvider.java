package io.quarkus.devtools.project.extensions;

import java.util.HashMap;
import java.util.Map;

public class ScmInfoProvider {

    public static Map<String, String> getSourceRepo() {
        // We could try and parse the .git/config file, but that will be fragile
        // Let's assume we only care about the repo for official-ish builds produced via github actions
        String repo = System.getenv("GITHUB_REPOSITORY");
        if (repo != null) {
            Map info = new HashMap();
            String qualifiedRepo = "https://github.com/" + repo;
            // Don't try and guess where slashes will be, just deal with any double slashes by brute force
            qualifiedRepo = qualifiedRepo.replace("github.com//", "github.com/");

            info.put("url", qualifiedRepo);
            return info;
        }
        return null;
    }

}
