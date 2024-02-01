package io.quarkus.it.panache;

import io.quarkus.hibernate.orm.panache.common.NestedProjectedClass;
import io.quarkus.hibernate.orm.panache.common.ProjectedFieldName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PersonDTO extends PersonName {

    public final AddressDTO address;

    public final DescriptionDTO description;

    @ProjectedFieldName("description.size")
    public final Integer directHeight;

    public PersonDTO(String uniqueName, String name, AddressDTO address, DescriptionDTO description,
            Integer directHeight) {
        super(uniqueName, name);
        this.address = address;
        this.description = description;
        this.directHeight = directHeight;
    }

    @NestedProjectedClass
    public static class AddressDTO implements Comparable<AddressDTO> {

        // Simple field with automatic mapping in constructor
        public final String street;

        public AddressDTO(String street) {
            this.street = street;

        }

        @Override
        public int compareTo(AddressDTO address) {
            return street.compareTo(address.street);
        }

    }

    @NestedProjectedClass
    public static class DescriptionDTO {
        private final String description;

        @ProjectedFieldName("size")
        public final Integer height;

        public DescriptionDTO(Integer height, Integer weight) {
            this.height = height;
            this.description = "Height: " + height + ", weight: " + weight;
        }

        public String getDescription() {
            return description;
        }
    }
}
