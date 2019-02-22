/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator.config.test;

import io.quarkus.creator.config.reader.PropertiesConfigReaderException;
import io.quarkus.creator.config.reader.PropertiesHandler;
import io.quarkus.creator.config.reader.PropertyContext;

/**
 *
 * @author Alexey Loubyansky
 */
public class SwitchTestCase extends PersonAddressTestBase {

    /**
     * Person properties handler
     */
    static final class PersonPropertiesHandler implements PropertiesHandler<Person> {

        @Override
        public Person getTarget() {
            return new Person();
        }

        @Override
        public boolean set(Person t, PropertyContext ctx) throws PropertiesConfigReaderException {
            switch (ctx.getRelativeName()) {
                case "first-name":
                    t.setFirstName(ctx.getValue());
                    break;
                case "last-name":
                    t.setLastName(ctx.getValue());
                    break;
                default:
                    return false;
            }
            return true;
        }

        @Override
        public PropertiesHandler<?> getNestedHandler(String name) throws PropertiesConfigReaderException {
            switch (name) {
                case "home-address":
                    return AddressPropertiesHandler.INSTANCE;
                case "work-address":
                    return AddressPropertiesHandler.INSTANCE;
            }
            return null;
        }

        @Override
        public void setNested(Person t, String name, Object child) throws PropertiesConfigReaderException {
            final Address address = ((Address.Builder) child).build();
            switch (name) {
                case "home-address":
                    t.setHomeAddress(address);
                    break;
                case "work-address":
                    t.setWorkAddress(address);
            }
        }
    }

    /**
     * Address properties handler
     */
    static class AddressPropertiesHandler implements PropertiesHandler<Address.Builder> {

        static final AddressPropertiesHandler INSTANCE = new AddressPropertiesHandler();

        @Override
        public Address.Builder getTarget() {
            return Address.builder();
        }

        @Override
        public boolean set(Address.Builder t, PropertyContext ctx) throws PropertiesConfigReaderException {
            switch (ctx.getRelativeName()) {
                case "street":
                    t.setStreet(ctx.getValue());
                    break;
                case "zip":
                    t.setZip(ctx.getValue());
                    break;
                case "city":
                    t.setCity(ctx.getValue());
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    @Override
    protected PropertiesHandler<Person> getPropertiesHandler() {
        return new PersonPropertiesHandler();
    }
}
