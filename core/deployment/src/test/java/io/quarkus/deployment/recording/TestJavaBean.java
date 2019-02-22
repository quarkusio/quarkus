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

package io.quarkus.deployment.recording;

import java.util.Objects;

public class TestJavaBean {

    public TestJavaBean() {
    }

    public TestJavaBean(String sval, int ival) {
        this.sval = sval;
        this.ival = ival;
    }

    private String sval;
    private int ival;

    public String getSval() {
        return sval;
    }

    public void setSval(String sval) {
        this.sval = sval;
    }

    public int getIval() {
        return ival;
    }

    public void setIval(int ival) {
        this.ival = ival;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TestJavaBean that = (TestJavaBean) o;
        return ival == that.ival &&
                Objects.equals(sval, that.sval);
    }

    @Override
    public int hashCode() {

        return Objects.hash(sval, ival);
    }

    @Override
    public String toString() {
        return "TestJavaBean{" +
                "sval='" + sval + '\'' +
                ", ival=" + ival +
                '}';
    }
}
