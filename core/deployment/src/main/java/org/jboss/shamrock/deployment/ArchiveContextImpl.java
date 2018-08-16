package org.jboss.shamrock.deployment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;

class ArchiveContextImpl implements ArchiveContext {

    private final ApplicationArchive root;
    private final Collection<ApplicationArchive> applicationArchives;
    private final BuildConfig buildConfig;
    private final IndexView combined;

    ArchiveContextImpl(ApplicationArchive root, Collection<ApplicationArchive> applicationArchives, BuildConfig buildConfig) {
        this.root = root;
        this.applicationArchives = applicationArchives;
        this.buildConfig = buildConfig;
        List<IndexView> allIndexes = new ArrayList<>();
        allIndexes.add(root.getIndex());
        for (ApplicationArchive i : applicationArchives) {
            allIndexes.add(i.getIndex());
        }
        combined = CompositeIndex.create(allIndexes);
    }

    @Override
    public ApplicationArchive getRootArchive() {
        return root;
    }

    @Override
    public Collection<ApplicationArchive> getApplicationArchives() {
        return Collections.unmodifiableCollection(applicationArchives);
    }

    @Override
    public Set<ApplicationArchive> getAllApplicationArchives() {
        HashSet<ApplicationArchive> ret = new HashSet<>(applicationArchives);
        ret.add(root);
        return Collections.unmodifiableSet(ret);
    }

    @Override
    public IndexView getCombinedIndex() {
        return combined;
    }

    @Override
    public BuildConfig getBuildConfig() {
        return buildConfig;
    }

    @Override
    public Set<Path> getDescriptors(String descriptor) {
        throw new RuntimeException("NYI: todo");
    }
}
