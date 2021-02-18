package io.quarkus.resteasy.reactive.server.deployment.devconsole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StaticResourceInfo {

    public Map<String, StaticFile> resourceMap = new TreeMap<>();

    public static final class StaticFile implements Comparable<StaticFile> {
        public String lastName;
        public boolean isFolder;
        public List<StaticFile> children = new ArrayList<>();

        public StaticFile(String lastName, boolean isFolder) {
            this.lastName = lastName;
            this.isFolder = isFolder;
        }

        @Override
        public int compareTo(StaticFile staticFile) {

            if (isFolder && !staticFile.isFolder) {
                //Folder before file
                return -1;
            } else if (!isFolder && staticFile.isFolder) {
                //File after folder
                return 1;
            } else
                //sort alphabetically
                return lastName.toLowerCase().compareTo(staticFile.lastName.toLowerCase());

        }
    }
}
