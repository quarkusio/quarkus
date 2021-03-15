package io.quarkus.deployment.ide;

public enum Ide {

    IDEA("idea"),
    ECLIPSE("eclipse"),
    VSCODE("code"),
    NETBEANS("netbeans");

    private String executable;

    private Ide(String executable) {
        this.executable = executable;
    }

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }
}
