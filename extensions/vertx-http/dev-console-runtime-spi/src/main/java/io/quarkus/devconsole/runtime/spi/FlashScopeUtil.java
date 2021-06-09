package io.quarkus.devconsole.runtime.spi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

public class FlashScopeUtil {
    private final static String FLASH_COOKIE_NAME = "_flash";
    private final static String FLASH_CONTEXT_DATA_NAME = "flash";

    public static void setFlash(RoutingContext event, Map<String, Object> data) {
        event.response().addCookie(
                Cookie.cookie(FLASH_COOKIE_NAME, Base64.getEncoder().encodeToString(marshallMap(data))));
    }

    public static Object getFlash(RoutingContext event) {
        return event.data().get(FLASH_CONTEXT_DATA_NAME);
    }

    public static void handleFlashCookie(RoutingContext event) {
        Cookie cookie = event.request().getCookie(FLASH_COOKIE_NAME);
        if (cookie != null) {
            byte[] bytes = cookie.getValue().getBytes();
            if (bytes != null && bytes.length != 0) {
                byte[] decoded = Base64.getDecoder().decode(bytes);
                // API says it can't be null
                if (decoded.length > 0) {
                    Map<String, Object> data = unmarshallMap(decoded);
                    event.data().put(FLASH_CONTEXT_DATA_NAME, data);
                }
            }
        }
        // must do this after we've read the value, otherwise we can't read it, for some reason
        event.response().removeCookie(FLASH_COOKIE_NAME);
    }

    // we don't use json because quarkus-vertx-http does not depend on Jackson databind and therefore the
    // stardard vert.x encoding methods fail

    private static byte[] marshallMap(Map<String, Object> data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(data);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unmarshallMap(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream oos = new ObjectInputStream(bais)) {
            return (Map<String, Object>) oos.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public enum FlashMessageStatus {
        SUCCESS("success"),
        ERROR("danger");

        public final String cssClass;

        FlashMessageStatus(String cssClass) {
            this.cssClass = cssClass;
        }
    }

    public static void setFlashMessage(RoutingContext event, String message) {
        setFlashMessage(event, message, FlashMessageStatus.SUCCESS, null);
    }

    public static void setFlashMessage(RoutingContext event, String message, Duration displayTime) {
        setFlashMessage(event, message, FlashMessageStatus.SUCCESS, displayTime);
    }

    public static void setFlashMessage(RoutingContext event, String message, FlashMessageStatus messageStatus,
            Duration displayTime) {
        Map<String, Object> data = new HashMap<>();
        Map<String, String> messageData = new HashMap<>();
        messageData.put("text", message);
        messageData.put("class", messageStatus.cssClass);
        data.put("message", messageData);
        data.put("displayTime", displayTime != null ? displayTime.toMillis() : 2000);
        setFlash(event, data);
    }
}
