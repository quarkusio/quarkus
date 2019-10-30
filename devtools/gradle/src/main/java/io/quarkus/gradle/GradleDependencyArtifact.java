package io.quarkus.gradle;

import org.gradle.api.artifacts.DependencyArtifact;

public class GradleDependencyArtifact implements DependencyArtifact {

    private String classifier;
    private String extension;
    private String name;
    private String type;
    private String url;

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setClassifier(String arg0) {
        this.classifier = arg0;
    }

    @Override
    public void setExtension(String arg0) {
        this.extension = arg0;
    }

    @Override
    public void setName(String arg0) {
        this.name = arg0;
    }

    @Override
    public void setType(String arg0) {
        this.type = arg0;
    }

    @Override
    public void setUrl(String arg0) {
        this.url = arg0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((extension == null) ? 0 : extension.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GradleDependencyArtifact other = (GradleDependencyArtifact) obj;
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        if (extension == null) {
            if (other.extension != null)
                return false;
        } else if (!extension.equals(other.extension))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "GradleDepArtifact [classifier=" + classifier + ", extension=" + extension + ", name=" + name + ", type=" + type
                + ", url=" + url + "]";
    }
}
