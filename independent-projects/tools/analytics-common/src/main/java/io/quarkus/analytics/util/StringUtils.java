package io.quarkus.analytics.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class StringUtils {

    private static final String CONCAT_DELIMITER = "; ";

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = (new ObjectMapper()).findAndRegisterModules();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Anonymize sensitive contents.
     * This is non-reversible.
     *
     * @param input Any String
     * @return human-readable deterministic gibberish String based on the input
     */
    public static String hashSHA256(final String input) {
        if (isBlank(input)) {
            return "4veeW2AzC7pMKJliIxtropV9CxTn3rMRBBcAPHnepjU="; // hashed N/A
        }
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return "N/A";
        }
    }

    private static boolean isBlank(String input) {
        return input == null || input.isBlank();
    }

    public static String concat(List<String> stringList) {
        if (stringList.isEmpty()) {
            return "N/A";
        }
        return stringList.stream().collect(Collectors.joining(CONCAT_DELIMITER));
    }
}
