/**
 *
 */
package io.quarkus.creator.config.test;

/**
 *
 * @author Alexey Loubyansky
 */
public class JavaVm {

    private String name;
    private String info;
    private String version;
    private JavaVmSpec spec;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public JavaVmSpec getSpec() {
        return spec;
    }

    public void setSpec(JavaVmSpec spec) {
        this.spec = spec;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((info == null) ? 0 : info.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((spec == null) ? 0 : spec.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        JavaVm other = (JavaVm) obj;
        if (info == null) {
            if (other.info != null)
                return false;
        } else if (!info.equals(other.info))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (spec == null) {
            if (other.spec != null)
                return false;
        } else if (!spec.equals(other.spec))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[name=" + name + ", info=" + info + ", version=" + version + ", spec=" + spec + "]";
    }
}
