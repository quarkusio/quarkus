package io.quarkus.bootstrap.model;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents an application (or its dependency) artifact.
 *
 * @author Alexey Loubyansky
 */
public class AppArtifact extends AppArtifactCoords implements Serializable {

    protected transient Path path;

    public AppArtifact(String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
    }

    public AppArtifact(String groupId, String artifactId, String classifier, String type, String version) {
        super(groupId, artifactId, classifier, type, version);
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public boolean isResolved() {
        return path != null;
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.defaultWriteObject();
        out.writeUTF(path.toAbsolutePath().toString());
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        path = Paths.get(in.readUTF());
    }
}
