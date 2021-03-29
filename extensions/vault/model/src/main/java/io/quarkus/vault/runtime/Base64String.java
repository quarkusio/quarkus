package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.StringHelper.bytesToString;
import static io.quarkus.vault.runtime.StringHelper.stringToBytes;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = Base64StringSerializer.class)
@JsonDeserialize(using = Base64StringDeserializer.class)
public class Base64String {

    private String value;

    public static byte[] toBytes(String s) {
        return getDecoder().decode(s);
    }

    public static Base64String from(String s) {
        return s == null ? null : new Base64String(getEncoder().encodeToString(stringToBytes(s)));
    }

    public static Base64String from(byte[] bytes) {
        return bytes == null ? null : new Base64String(getEncoder().encodeToString(bytes));
    }

    public Base64String(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public byte[] decodeAsBytes() {
        return toBytes(value);
    }

    public String decodeAsString() {
        return bytesToString(decodeAsBytes());
    }

    @Override
    public String toString() {
        return value;
    }

}
