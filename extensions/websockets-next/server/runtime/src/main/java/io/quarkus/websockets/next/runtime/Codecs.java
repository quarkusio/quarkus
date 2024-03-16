package io.quarkus.websockets.next.runtime;

import java.lang.reflect.Type;
import java.util.List;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.All;
import io.quarkus.websockets.next.BinaryMessageCodec;
import io.quarkus.websockets.next.MessageCodec;
import io.quarkus.websockets.next.TextMessageCodec;
import io.vertx.core.buffer.Buffer;

@Singleton
public class Codecs {

    private static final Logger LOG = Logger.getLogger(Codecs.class);

    @All
    List<TextMessageCodec<?>> textCodecs;

    @All
    List<BinaryMessageCodec<?>> binaryCodecs;

    public Object textDecode(Type type, String value, Class<?> codecBeanClass) {
        if (codecBeanClass != null) {
            for (TextMessageCodec<?> codec : textCodecs) {
                if (codec.getClass().equals(codecBeanClass)) {
                    if (!codec.supports(type)) {
                        throw forcedCannotHandle(false, codec, type);
                    }
                    try {
                        return codec.decode(type, value);
                    } catch (Exception e) {
                        throw unableToDecode(false, codec, e);
                    }
                }
            }
        } else {
            for (TextMessageCodec<?> codec : textCodecs) {
                if (codec.supports(type)) {
                    try {
                        return codec.decode(type, value);
                    } catch (Exception e) {
                        throw unableToDecode(false, codec, e);
                    }
                }
            }
        }

        throw noCodec(false, type);
    }

    public <T> String textEncode(T message, Class<?> codecBeanClass) {
        Class<?> type = message.getClass();
        if (codecBeanClass != null) {
            for (TextMessageCodec<?> codec : textCodecs) {
                if (codec.getClass().equals(codecBeanClass)) {
                    if (!codec.supports(type)) {
                        throw forcedCannotHandle(false, codec, type);
                    }
                    try {
                        return codec.encode(cast(message));
                    } catch (Exception e) {
                        throw unableToEncode(false, codec, e);
                    }
                }
            }
        } else {
            for (TextMessageCodec<?> codec : textCodecs) {
                if (codec.supports(type)) {
                    try {
                        return codec.encode(cast(message));
                    } catch (Exception e) {
                        throw unableToEncode(false, codec, e);
                    }
                }
            }
        }
        throw noCodec(false, type);
    }

    public Object binaryDecode(Type type, Buffer value, Class<?> codecBeanClass) {
        if (codecBeanClass != null) {
            for (BinaryMessageCodec<?> codec : binaryCodecs) {
                if (codec.getClass().equals(codecBeanClass)) {
                    if (!codec.supports(type)) {
                        throw forcedCannotHandle(false, codec, type);
                    }
                    try {
                        return codec.decode(type, value);
                    } catch (Exception e) {
                        throw unableToDecode(false, codec, e);
                    }
                }
            }
        } else {
            for (BinaryMessageCodec<?> codec : binaryCodecs) {
                if (codec.supports(type)) {
                    try {
                        return codec.decode(type, value);
                    } catch (Exception e) {
                        LOG.errorf(e, "Unable to decode binary message with %s", codec.getClass().getName());
                    }
                }
            }
        }
        throw noCodec(true, type);
    }

    public <T> Buffer binaryEncode(T message, Class<?> codecBeanClass) {
        Class<?> type = message.getClass();
        if (codecBeanClass != null) {
            for (BinaryMessageCodec<?> codec : binaryCodecs) {
                if (codec.getClass().equals(codecBeanClass)) {
                    if (!codec.supports(type)) {
                        throw forcedCannotHandle(false, codec, type);
                    }
                    try {
                        return codec.encode(cast(message));
                    } catch (Exception e) {
                        throw unableToEncode(false, codec, e);
                    }
                }
            }
        } else {
            for (BinaryMessageCodec<?> codec : binaryCodecs) {
                if (codec.supports(type)) {
                    try {
                        return codec.encode(cast(message));
                    } catch (Exception e) {
                        throw unableToEncode(true, codec, e);
                    }
                }
            }
        }
        throw noCodec(true, type);
    }

    IllegalStateException noCodec(boolean binary, Type type) {
        String message = String.format("No %s codec handles the type %s", binary ? "binary" : "text", type);
        throw new IllegalStateException(message);
    }

    IllegalStateException unableToEncode(boolean binary, MessageCodec<?, ?> codec, Exception e) {
        String message = String.format("Unable to encode %s message with %s", binary ? "binary" : "text",
                codec.getClass().getName());
        throw new IllegalStateException(message, e);
    }

    IllegalStateException unableToDecode(boolean binary, MessageCodec<?, ?> codec, Exception e) {
        String message = String.format("Unable to decode %s message with %s", binary ? "binary" : "text",
                codec.getClass().getName());
        throw new IllegalStateException(message, e);
    }

    IllegalStateException forcedCannotHandle(boolean binary, MessageCodec<?, ?> codec, Type type) {
        throw new IllegalStateException(
                String.format("Forced %s codec [%s] cannot handle the type %s", binary ? "binary" : "text",
                        codec.getClass().getName(), type));
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object obj) {
        return (T) obj;
    }
}
