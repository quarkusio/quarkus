/**
 *
 */
package io.quarkus.creator.config.test;

/**
 *
 * @author Alexey Loubyansky
 */
public class JavaInfo {

    private String version;
    private JavaVm vm;

    public JavaVm getVM() {
        return vm;
    }

    public void setVM(JavaVm vm) {
        this.vm = vm;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((vm == null) ? 0 : vm.hashCode());
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
        JavaInfo other = (JavaInfo) obj;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        if (vm == null) {
            if (other.vm != null)
                return false;
        } else if (!vm.equals(other.vm))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[version=" + version + ", vm=" + vm + "]";
    }
}
