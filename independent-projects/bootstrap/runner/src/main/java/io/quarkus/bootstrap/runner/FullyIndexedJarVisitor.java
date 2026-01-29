package io.quarkus.bootstrap.runner;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

class FullyIndexedJarVisitor implements JarVisitor {

    private final List<String> fullyIndexedDirectories;
    private final Set<String> fullyIndexedResources = new LinkedHashSet<>();

    FullyIndexedJarVisitor(List<String> fullyIndexedDirectories) {
        this.fullyIndexedDirectories = fullyIndexedDirectories;
    }

    public Set<String> getFullyIndexedResources() {
        return fullyIndexedResources;
    }

    @Override
    public void visitJarFileEntry(JarFile jarFile, ZipEntry entry) {
        // collect data for fully indexed directories, we collect both files and directories present there
        for (String prefix : fullyIndexedDirectories) {
            if (prefix.isEmpty()) {
                int firstSlash = entry.getName().indexOf('/');
                String topLevel = (firstSlash == -1) ? entry.getName() : entry.getName().substring(0, firstSlash);
                fullyIndexedResources.add(topLevel);
                continue;
            }

            String slashedPrefix = prefix + '/';

            if (!entry.getName().startsWith(slashedPrefix)) {
                continue;
            }

            if (entry.getName().equals(slashedPrefix)) {
                fullyIndexedResources.add(prefix);
                continue;
            }

            int nextSlash = entry.getName().indexOf('/', slashedPrefix.length());
            String result;

            if (nextSlash != -1) {
                // we index the subdirectory part only
                result = entry.getName().substring(0, nextSlash);
            } else {
                // It's a file or directory at the current level
                result = entry.getName();
            }

            fullyIndexedResources.add(result);
        }
    }
}
