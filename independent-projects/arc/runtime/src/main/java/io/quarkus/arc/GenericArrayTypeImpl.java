/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

/**
 * @author Marko Luksa
 * @author Jozef Hartinger
 */
public class GenericArrayTypeImpl implements GenericArrayType {

    private Type genericComponentType;

    public GenericArrayTypeImpl(Type genericComponentType) {
        this.genericComponentType = genericComponentType;
    }

    public GenericArrayTypeImpl(Class<?> rawType, Type... actualTypeArguments) {
        this.genericComponentType = new ParameterizedTypeImpl(rawType, actualTypeArguments);
    }

    @Override
    public Type getGenericComponentType() {
        return genericComponentType;
    }

    @Override
    public int hashCode() {
        return ((genericComponentType == null) ? 0 : genericComponentType.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GenericArrayType) {
            GenericArrayType that = (GenericArrayType) obj;
            if (genericComponentType == null) {
                return that.getGenericComponentType() == null;
            } else {
                return genericComponentType.equals(that.getGenericComponentType());
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(genericComponentType.toString());
        sb.append("[]");
        return sb.toString();
    }
}
