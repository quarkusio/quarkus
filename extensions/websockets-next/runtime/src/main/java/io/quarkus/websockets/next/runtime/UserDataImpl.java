package io.quarkus.websockets.next.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.quarkus.websockets.next.UserData;

final class UserDataImpl implements UserData {

    private final ConcurrentMap<String, Object> data;

    UserDataImpl() {
        this.data = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <VALUE> VALUE get(TypedKey<VALUE> key) {
        return (VALUE) data.get(key.value());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <VALUE> VALUE put(TypedKey<VALUE> key, VALUE value) {
        return (VALUE) data.put(key.value(), value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <VALUE> VALUE remove(TypedKey<VALUE> key) {
        return (VALUE) data.remove(key.value());
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public int size() {
        return data.size();
    }

}
