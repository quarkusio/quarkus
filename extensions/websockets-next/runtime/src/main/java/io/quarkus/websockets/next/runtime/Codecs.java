package io.quarkus.websockets.next.runtime;

import java.lang.reflect.Type;
import java.util.List;

import jakarta.inject.Singleton;

import io.quarkus.arc.All;
import io.quarkus.websockets.next.BinaryDecodeException;
import io.quarkus.websockets.next.BinaryEncodeException;
import io.quarkus.websockets.next.BinaryMessageCodec;
import io.quarkus.websockets.next.MessageCodec;
import io.quarkus.websockets.next.TextDecodeException;
import io.quarkus.websockets.next.TextEncodeException;
import io.quarkus.websockets.next.TextMessageCodec;
import io.quarkus.websockets.next.WebSocketException;
import io.vertx.core.buffer.Buffer;

@Singleton
public class Codecs {

    @All
    List<TextMessageCodec<?>> textCodecs;

    @All
    List<BinaryMessageCodec<?>> binaryCodecs;

    public Object textDecode(Type type, String value, Class<?> codecBeanClass) {
        if (codecBeanClass != null) {
            for (TextMessageCodec<?> codec : textCodecs) {
                if (codec.getClass().equals(codecBeanClass)) {
                    if (!codec.supports(type)) {
                        throw forcedCannotDecode(value, null, codec, type);
                    }
                    try {
                        return codec.decode(type, value);
                    } catch (Exception e) {
                        throw unableToDecode(value, null, codec, e);
                    }
                }
            }
        } else {
            for (TextMessageCodec<?> codec : textCodecs) {
                if (codec.supports(type)) {
                    try {
                        return codec.decode(type, value);
                    } catch (Exception e) {
                        throw unableToDecode(value, null, codec, e);
                    }
                }
            }
        }
        throw noCodecToDecode(value, null, type);
    }

    public <T> String textEncode(T message, Class<?> codecBeanClass) {
        Class<?> type = message.getClass();
        if (codecBeanClass != null) {
            for (TextMessageCodec<?> codec : textCodecs) {
                if (codec.getClass().equals(codecBeanClass)) {
                    if (!codec.supports(type)) {
                        throw forcedCannotEncode(false, codec, message);
                    }
                    try {
                        return codec.encode(cast(message));
                    } catch (Exception e) {
                        throw unableToEncode(false, codec, message, e);
                    }
                }
            }
        } else {
            for (TextMessageCodec<?> codec : textCodecs) {
                if (codec.supports(type)) {
                    try {
                        return codec.encode(cast(message));
                    } catch (Exception e) {
                        throw unableToEncode(false, codec, message, e);
                    }
                }
            }
        }
        throw noCodecToEncode(false, message, type);
    }

    public Object binaryDecode(Type type, Buffer value, Class<?> codecBeanClass) {
        if (codecBeanClass != null) {
            for (BinaryMessageCodec<?> codec : binaryCodecs) {
                if (codec.getClass().equals(codecBeanClass)) {
                    if (!codec.supports(type)) {
                        throw forcedCannotDecode(null, value, codec, type);
                    }
                    try {
                        return codec.decode(type, value);
                    } catch (Exception e) {
                        throw unableToDecode(null, value, codec, e);
                    }
                }
            }
        } else {
            for (BinaryMessageCodec<?> codec : binaryCodecs) {
                if (codec.supports(type)) {
                    try {
                        return codec.decode(type, value);
                    } catch (Exception e) {
                        throw unableToDecode(null, value, codec, e);
                    }
                }
            }
        }
        throw noCodecToDecode(null, value, type);
    }

    public <T> Buffer binaryEncode(T message, Class<?> codecBeanClass) {
        Class<?> type = message.getClass();
        if (codecBeanClass != null) {
            for (BinaryMessageCodec<?> codec : binaryCodecs) {
                if (codec.getClass().equals(codecBeanClass)) {
                    if (!codec.supports(type)) {
                        throw forcedCannotEncode(true, codec, message);
                    }
                    try {
                        return codec.encode(cast(message));
                    } catch (Exception e) {
                        throw unableToEncode(true, codec, message, e);
                    }
                }
            }
        } else {
            for (BinaryMessageCodec<?> codec : binaryCodecs) {
                if (codec.supports(type)) {
                    try {
                        return codec.encode(cast(message));
                    } catch (Exception e) {
                        throw unableToEncode(true, codec, message, e);
                    }
                }
            }
        }
        throw noCodecToEncode(true, message, type);
    }

    WebSocketException noCodecToDecode(String text, Buffer bytes, Type type) {
        String message = String.format("No %s codec handles the type %s", bytes != null ? "binary" : "text", type);
        if (bytes != null) {
            return new BinaryDecodeException(bytes, message);
        } else {
            return new TextDecodeException(text, message);
        }
    }

    WebSocketException noCodecToEncode(boolean binary, Object encodedObject, Type type) {
        String message = String.format("No %s codec handles the type %s", binary ? "binary" : "text", type);
        if (binary) {
            return new BinaryEncodeException(encodedObject, message);
        } else {
            return new TextEncodeException(encodedObject, message);
        }
    }

    WebSocketException unableToEncode(boolean binary, MessageCodec<?, ?> codec, Object encodedObject, Exception e) {
        String message = String.format("Unable to encode %s message with %s", binary ? "binary" : "text",
                codec.getClass().getName());
        if (binary) {
            return new BinaryEncodeException(encodedObject, message, e);
        } else {
            return new TextEncodeException(encodedObject, message, e);
        }
    }

    WebSocketException unableToDecode(String text, Buffer bytes, MessageCodec<?, ?> codec, Exception e) {
        String message = String.format("Unable to decode %s message with %s", bytes != null ? "binary" : "text",
                codec.getClass().getName());
        if (bytes != null) {
            return new BinaryDecodeException(bytes, message, e);
        } else {
            return new TextDecodeException(text, message, e);
        }
    }

    WebSocketException forcedCannotEncode(boolean binary, MessageCodec<?, ?> codec, Object encodedObject) {
        String message = String.format("Forced %s codec [%s] cannot handle the type %s", binary ? "binary" : "text",
                codec.getClass().getName(), encodedObject.getClass());
        if (binary) {
            return new BinaryEncodeException(encodedObject, message);
        } else {
            return new TextEncodeException(encodedObject, message);
        }
    }

    WebSocketException forcedCannotDecode(String text, Buffer bytes, MessageCodec<?, ?> codec, Type type) {
        String message = String.format("Forced %s codec [%s] cannot decode the type %s", bytes != null ? "binary" : "text",
                codec.getClass().getName(), type);
        if (bytes != null) {
            return new BinaryDecodeException(bytes, message);
        } else {
            return new TextDecodeException(text, message);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T cast(Object obj) {
        return (T) obj;
    }
}
