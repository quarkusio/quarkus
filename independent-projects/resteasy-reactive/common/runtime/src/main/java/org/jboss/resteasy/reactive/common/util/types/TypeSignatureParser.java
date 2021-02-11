package org.jboss.resteasy.reactive.common.util.types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// FIXME: move to quarkus-core?
public class TypeSignatureParser {

    private char[] chars;
    private int i;

    public TypeSignatureParser(String signature) {
        chars = signature.toCharArray();
    }

    public Type parseType() {
        int arrayCount = 0;
        Type ret = null;
        int start = i;
        LOOP: do {
            char c = chars[i++];
            switch (c) {
                // BaseType
                case 'B':
                    ret = byte.class;
                    break LOOP;
                case 'C':
                    ret = char.class;
                    break LOOP;
                case 'D':
                    ret = double.class;
                    break LOOP;
                case 'F':
                    ret = float.class;
                    break LOOP;
                case 'I':
                    ret = int.class;
                    break LOOP;
                case 'J':
                    ret = long.class;
                    break LOOP;
                case 'S':
                    ret = short.class;
                    break LOOP;
                case 'Z':
                    ret = boolean.class;
                    break LOOP;
                case 'V':
                    ret = void.class;
                    break LOOP;
                // ClassTypeSignature
                case 'L':
                    ret = parseReference();
                    break LOOP;
                // TypeVariableSignature
                case 'T':
                    // Stef has come to the conclusion that because TypeVariable depends on the GenericDeclaration that defined them, which is lacking
                    // in signatures unless we have access to the current context, we should not support them
                    throw new IllegalArgumentException(
                            "Invalid type variable in signature: " + new String(chars) + " at position " + i);
                //                // consume until the ;
                //                int tStart = i;
                //                while(chars[i++] != ';') {
                //                }
                //                ret = new FixedTypeVariableImpl<>(new String(chars, tStart, i - tStart - 1));
                //                break LOOP;
                // ArrayTypeSignature
                case '[':
                    arrayCount++;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Invalid signature char: " + c + " in " + new String(chars) + " at position " + i);
            }
        } while (true);
        if (arrayCount > 0) {
            if (ret instanceof Class) {
                Class<?> retClass = (Class<?>) ret;
                if (retClass.isPrimitive()) {
                    // load [I or [[I
                    return loadClass(new String(chars, start, i - start));
                }
                // get the [L part and the name out of the class
                return loadClass(new String(chars, start, arrayCount + 1) + retClass.getName() + ";");
            }
            if (ret instanceof ParameterizedType) {
                // this is a moronic API
                for (int a = 0; a < arrayCount; a++) {
                    ret = new GenericArrayTypeImpl(ret);
                }
                return ret;
            }
            throw new UnsupportedOperationException();
        }
        return ret;
    }

    private Type parseReference() {
        int start = i;
        StringBuilder name = new StringBuilder(chars.length - start);
        List<Type> args = new ArrayList<>();
        Type lastOwnerType = null;
        while (i < chars.length) {
            char c = chars[i++];
            switch (c) {
                // last part was PackageSpecifier
                case '/':
                    name.append('.');
                    break;
                // last part was Identifier
                case '<':
                    do {
                        boolean ext = false;
                        boolean sup = false;
                        if (chars[i] == '+') {
                            i++;
                            ext = true;
                        } else if (chars[i] == '-') {
                            i++;
                            sup = true;
                        }
                        Type typeArg;
                        if (ext) {
                            typeArg = WildcardTypeImpl.withUpperBound(parseType());
                        } else if (sup) {
                            typeArg = WildcardTypeImpl.withLowerBound(parseType());
                        } else if (chars[i] == '*') {
                            i++;
                            typeArg = WildcardTypeImpl.defaultInstance();
                        } else {
                            typeArg = parseType();
                        }
                        args.add(typeArg);
                        if (chars[i] == '>') {
                            i++;
                            break;
                        }
                    } while (true);
                    break;
                // Inner type
                case '.':
                    Type ownerType = loadClass(name.toString());
                    if (!args.isEmpty()) {
                        ownerType = new ParameterizedTypeImpl(ownerType, args.toArray(new Type[0]), lastOwnerType);
                        args.clear();
                    }
                    lastOwnerType = ownerType;
                    name.append('$');
                    break;
                // last part was Identifier
                case ';':
                    Type type = loadClass(name.toString());
                    if (args.isEmpty())
                        return type;
                    return new ParameterizedTypeImpl(type, args.toArray(new Type[0]), lastOwnerType);
                // default is text
                default:
                    name.append(c);
                    break;
            }
        }
        throw new UnsupportedOperationException();
    }

    private Type loadClass(String name) {
        try {
            // VERY IMPORTANT: we use this for array types, so don't change Class.forName into ClassLoader.loadClass
            // which does not support arrays
            return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Type parse(String signature) {
        return new TypeSignatureParser(signature).parseType();
    }
}
