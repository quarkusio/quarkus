package io.quarkus.hibernate.validator.test.valueextractor;

// TODO for some reason, this must be a top-level type, not a nested/inner type,
//  in order for annotations on type parameters to be detected.
//  See NestedContainerTypeCustomValueExtractorTest
public class Container<T> {

    public T value;

}
