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
