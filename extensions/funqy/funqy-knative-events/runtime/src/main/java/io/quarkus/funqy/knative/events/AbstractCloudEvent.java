package io.quarkus.funqy.knative.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractCloudEvent<T> implements CloudEvent<T> {

    @Override
    public String toString() {
        return "CloudEvent{" +
                "specVersion='" + specVersion() + '\'' +
                ", id='" + id() + '\'' +
                ", type='" + type() + '\'' +
                ", source='" + source() + '\'' +
                ", subject='" + subject() + '\'' +
                ", time=" + time() +
                ", extensions=" + extensions() +
                ", dataSchema=" + dataSchema() +
                ", dataContentType='" + dataContentType() + '\'' +
                ", data=" + data() +
                '}';
    }

    private static final Pattern VERSION_REGEX = Pattern.compile("^(\\d+)\\.(\\d+)$");

    public static int parseMajorSpecVersion(String ver) {
        if (ver == null) {
            return -1;
        }
        Matcher m = VERSION_REGEX.matcher(ver);
        if (m.find()) {
            ver = m.group(1);
            if (ver == null) {
                return -1;
            }
            try {
                return Integer.parseInt(ver);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public static boolean isKnownSpecVersion(String ceSpecVersion) {
        int maj = parseMajorSpecVersion(ceSpecVersion);
        return maj == 0 || maj == 1;
    }

    private boolean isMajorParsed;
    private int majorSpecVersion;

    protected int majorSpecVersion() {
        if (!isMajorParsed) {
            majorSpecVersion = parseMajorSpecVersion(specVersion());
            isMajorParsed = true;
        }
        return majorSpecVersion;
    }
}
