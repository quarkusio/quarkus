package io.quarkus.deployment.index;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ConstPoolScanner {

    static final int CONSTANT_UTF8_TAG = 1;
    static final int CONSTANT_INTEGER_TAG = 3;
    static final int CONSTANT_FLOAT_TAG = 4;
    static final int CONSTANT_LONG_TAG = 5;
    static final int CONSTANT_DOUBLE_TAG = 6;
    static final int CONSTANT_CLASS_TAG = 7;
    static final int CONSTANT_STRING_TAG = 8;
    static final int CONSTANT_FIELDREF_TAG = 9;
    static final int CONSTANT_METHODREF_TAG = 10;
    static final int CONSTANT_INTERFACE_METHODREF_TAG = 11;
    static final int CONSTANT_NAME_AND_TYPE_TAG = 12;
    static final int CONSTANT_METHOD_HANDLE_TAG = 15;
    static final int CONSTANT_METHOD_TYPE_TAG = 16;
    static final int CONSTANT_DYNAMIC_TAG = 17;
    static final int CONSTANT_INVOKE_DYNAMIC_TAG = 18;
    static final int CONSTANT_MODULE_TAG = 19;
    static final int CONSTANT_PACKAGE_TAG = 20;

    //TODO: at the moment this only looks for the class name, it does not make sure it is used
    //by a class tag. In practice this is likely fine, especially as this is just used for optimisations.
    public static boolean constPoolEntryPresent(byte[] classBody, Set<String> namesToLookFor) {
        ByteBuffer data = ByteBuffer.wrap(classBody);
        if (data.getInt() != 0xCAFEBABE) {
            return false; //not a class file
        }
        data.getShort();//major
        data.getShort();//minor
        int constantPoolCount = data.getShort();
        int currentCpInfoIndex = 1;
        while (currentCpInfoIndex < constantPoolCount) {
            currentCpInfoIndex++;
            int cpInfoSize;
            switch (data.get()) {
                case CONSTANT_FIELDREF_TAG:
                case CONSTANT_METHODREF_TAG:
                case CONSTANT_INTERFACE_METHODREF_TAG:
                case CONSTANT_INTEGER_TAG:
                case CONSTANT_FLOAT_TAG:
                case CONSTANT_NAME_AND_TYPE_TAG:
                    cpInfoSize = 4;
                    break;
                case CONSTANT_DYNAMIC_TAG:
                    cpInfoSize = 4;
                    break;
                case CONSTANT_INVOKE_DYNAMIC_TAG:
                    cpInfoSize = 4;
                    break;
                case CONSTANT_LONG_TAG:
                case CONSTANT_DOUBLE_TAG:
                    cpInfoSize = 8;
                    currentCpInfoIndex++;
                    break;
                case CONSTANT_UTF8_TAG:
                    int strLength = 0xFFFF & data.getShort();
                    cpInfoSize = 0;
                    byte[] str = new byte[strLength];
                    data.get(str);
                    if (namesToLookFor.contains(new String(str, StandardCharsets.UTF_8))) {
                        return true;
                    }
                    break;
                case CONSTANT_METHOD_HANDLE_TAG:
                    cpInfoSize = 3;
                    break;
                case CONSTANT_CLASS_TAG:
                case CONSTANT_STRING_TAG:
                case CONSTANT_METHOD_TYPE_TAG:
                case CONSTANT_PACKAGE_TAG:
                case CONSTANT_MODULE_TAG:
                    cpInfoSize = 2;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            for (int j = 0; j < cpInfoSize; ++j) {
                data.get();
            }
        }

        return false;

    }

}
