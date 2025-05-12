package io.quarkus.arc.runtime.dev;

import java.util.List;

public class EventInfo {
    private String timestamp;
    private String type;
    private List<String> qualifiers;
    private boolean isContextEvent;

    public EventInfo() {
    }

    public EventInfo(String timestamp, String type, List<String> qualifiers, boolean isContextEvent) {
        this.timestamp = timestamp;
        this.type = type;
        this.qualifiers = qualifiers;
        this.isContextEvent = isContextEvent;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(List<String> qualifiers) {
        this.qualifiers = qualifiers;
    }

    public boolean isIsContextEvent() {
        return isContextEvent;
    }

    public void setIsContextEvent(boolean isContextEvent) {
        this.isContextEvent = isContextEvent;
    }

}
