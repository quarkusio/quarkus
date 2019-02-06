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

package org.jboss.shamrock.example.panache;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class AccessorEntity extends GenericEntity<Integer> {

    public String string;
    public char c;
    public boolean bool;
    public byte b;
    public short s;
    public int i;
    public long l;
    public float f;
    public double d;
    transient public Object trans1;
    @Transient
    public Object trans2;
    
    public void method() {
        // touch some fields
        b = 2;
        System.err.println(i);
        t = 1;
        t2 = 2;
    }
    
    // explicit getter or setter
    
    public byte getB() {
        return b;
    }
    
    public void setI(int i) {
        this.i = i;
    }
}
