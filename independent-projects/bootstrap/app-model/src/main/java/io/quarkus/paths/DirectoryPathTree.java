package io.quarkus.paths;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectoryPathTree extends OpenContainerPathTree implements Serializable {

    private static final long serialVersionUID = 2255956884896445059L;

    private Path dir;

    /**
     * For deserialization
     */
    public DirectoryPathTree() {
        super();
    }

    public DirectoryPathTree(Path dir) {
        this(dir, null);
    }

    public DirectoryPathTree(Path dir, PathFilter pathFilter) {
        this(dir, pathFilter, false);
    }

    public DirectoryPathTree(Path dir, PathFilter pathFilter, boolean manifestEnabled) {
        super(pathFilter, manifestEnabled);
        this.dir = dir;
    }

    protected DirectoryPathTree(Path dir, PathFilter pathFilter, PathTreeWithManifest pathTreeWithManifest) {
        super(pathFilter, pathTreeWithManifest);
        this.dir = dir;
    }

    @Override
    protected Path getRootPath() {
        return dir;
    }

    @Override
    protected Path getContainerPath() {
        return dir;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public PathTree getOriginalTree() {
        return this;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(dir.toAbsolutePath().toString());
        out.writeObject(pathFilter);
        out.writeBoolean(manifestEnabled);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        dir = Paths.get(in.readUTF());
        pathFilter = (PathFilter) in.readObject();
        manifestEnabled = in.readBoolean();
    }
}
