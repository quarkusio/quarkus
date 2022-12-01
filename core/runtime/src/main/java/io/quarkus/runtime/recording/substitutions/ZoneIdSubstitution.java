package io.quarkus.runtime.recording.substitutions;

import java.time.ZoneId;

import io.quarkus.runtime.ObjectSubstitution;

public class ZoneIdSubstitution implements ObjectSubstitution<ZoneId, String> {

    @Override
    public String serialize(ZoneId obj) {
        return obj.getId();
    }

    @Override
    public ZoneId deserialize(String str) {
        return ZoneId.of(str);
    }
}
