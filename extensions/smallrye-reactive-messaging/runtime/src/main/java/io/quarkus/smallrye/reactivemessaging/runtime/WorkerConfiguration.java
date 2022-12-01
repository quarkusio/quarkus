package io.quarkus.smallrye.reactivemessaging.runtime;

public class WorkerConfiguration {

    private String className;

    private String methodName;

    private String poolName;

    public WorkerConfiguration() {
    }

    public WorkerConfiguration(String className, String name, String poolName) {
        this.className = className;
        this.methodName = name;
        this.poolName = poolName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

}
