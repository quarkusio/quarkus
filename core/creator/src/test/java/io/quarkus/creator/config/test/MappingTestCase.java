package io.quarkus.creator.config.test;

import io.quarkus.creator.config.reader.MappedPropertiesHandler;
import io.quarkus.creator.config.reader.PropertiesHandler;

/**
 *
 * @author Alexey Loubyansky
 */
public class MappingTestCase extends PersonAddressTestBase {

    @Override
    protected PropertiesHandler<Person> getPropertiesHandler() {

        /*
         * Map address builder to properties
         * Address.Builder is an example of building an immutable object
         */
        final PropertiesHandler<Address.Builder> addressHandler = new MappedPropertiesHandler<Address.Builder>() {
            @Override
            public Address.Builder getTarget() {
                return Address.builder();
            }
        }
                .map("street", (Address.Builder t, String value) -> t.setStreet(value))
                .map("zip", (Address.Builder t, String value) -> t.setZip(value))
                .map("city", (Address.Builder t, String value) -> t.setCity(value));

        /*
         * Map Person to properties
         */
        final PropertiesHandler<Person> personHandler = new MappedPropertiesHandler<Person>() {
            @Override
            public Person getTarget() {
                return new Person();
            }
        }
                .map("first-name", (Person p, String value) -> p.setFirstName(value))
                .map("last-name", (Person p, String value) -> p.setLastName(value))
                .map("home-address", addressHandler, (Person p, Address.Builder a) -> p.setHomeAddress(a.build()))
                .map("work-address", addressHandler, (Person p, Address.Builder a) -> p.setWorkAddress(a.build()));

        return personHandler;
    }
}
