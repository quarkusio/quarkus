package io.quarkus.qrs.runtime.client;

import java.util.Objects;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEvent;

public class QrsInboundSseEvent implements InboundSseEvent {

    private String id;
    private String name;
    private String comment;
    private String data;
    private long reconnectDelay = SseEvent.RECONNECT_NOT_SET;

    @Override
    public String getId() {
        return id;
    }

    public QrsInboundSseEvent setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public QrsInboundSseEvent setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public QrsInboundSseEvent setComment(String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public long getReconnectDelay() {
        return reconnectDelay;
    }

    @Override
    public boolean isReconnectDelaySet() {
        return reconnectDelay != -1;
    }

    public QrsInboundSseEvent setReconnectDelay(long reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
        return this;
    }

    @Override
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    @Override
    public String readData() {
        return data;
    }

    public QrsInboundSseEvent setData(String data) {
        this.data = data;
        return this;
    }

    @Override
    public <T> T readData(Class<T> type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readData(GenericType<T> type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readData(Class<T> messageType, MediaType mediaType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readData(GenericType<T> type, MediaType mediaType) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String toString() {
        return "InboundSseEvent[data: "+data
                +", name: "+name
                +", id: "+id
                +", comment: "+comment
                +", reconnectDelay: "+reconnectDelay
                +"]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hash(comment, data, id, name);
        result = prime * result + (int) (reconnectDelay ^ (reconnectDelay >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (obj instanceof InboundSseEvent == false)
            return false;
        InboundSseEvent other = (InboundSseEvent) obj;
        return Objects.equals(getComment(), other.getComment())
                && Objects.equals(getId(), other.getId())
                && Objects.equals(getName(), other.getName())
                && getReconnectDelay() == other.getReconnectDelay()
                && Objects.equals(readData(), other.readData());
    }
}
