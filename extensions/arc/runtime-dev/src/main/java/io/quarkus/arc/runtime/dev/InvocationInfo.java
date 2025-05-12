package io.quarkus.arc.runtime.dev;

import java.util.List;

public class InvocationInfo {

    private String startTime;

    private String methodName;

    // in milliseconds
    private long duration;

    // business method, producer, observer, etc.
    private String kind;

    private List<InvocationInfo> children;

    private boolean quarkusBean;

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<InvocationInfo> getChildren() {
        return children;
    }

    public void setChildren(List<InvocationInfo> children) {
        this.children = children;
    }

    public boolean isQuarkusBean() {
        return quarkusBean;
    }

    public void setQuarkusBean(boolean quarkusBean) {
        this.quarkusBean = quarkusBean;
    }

}
