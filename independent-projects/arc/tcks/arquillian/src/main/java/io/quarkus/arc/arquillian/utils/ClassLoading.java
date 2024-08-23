package io.quarkus.arc.arquillian.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ClassLoading {
    public static Class<?>[] convertToTCCL(Class<?>[] classes) throws ClassNotFoundException {
        return convertToCL(classes, Thread.currentThread().getContextClassLoader());
    }

    public static Class<?>[] convertToCL(Class<?>[] classes, ClassLoader classLoader) throws ClassNotFoundException {
        Class<?>[] result = new Class<?>[classes.length];
        for (int i = 0; i < classes.length; i++) {
            if (classes[i].getClassLoader() != classLoader) {
                result[i] = classLoader.loadClass(classes[i].getName());
            } else {
                result[i] = classes[i];
            }
        }
        return result;
    }

    public static Throwable cloneExceptionIntoSystemCL(Throwable exception) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream serializer = new ObjectOutputStream(out);
            serializer.writeObject(exception);
            serializer.close();
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            ObjectInputStream deserializer = new ObjectInputStream(in);
            return (Throwable) deserializer.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // shrug
            return exception;
        }
    }
}
