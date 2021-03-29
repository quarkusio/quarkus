package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.api.AbstractStringAssert;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;

public class MethodNameParserTest {

    private final Class<?> repositoryClass = PersonRepository.class;
    private final Class<?> entityClass = Person.class;
    private final Class[] additionalClasses = new Class[] { Person.Address.class, Person.Country.class };

    @Test
    public void testFindAllByAddressZipCode() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByAddressZipCode", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery()).isEqualTo("FROM Person WHERE address.zipCode = ?1");
        assertThat(result.getParamCount()).isEqualTo(1);
    }

    @Test
    public void testFindAllByAddressCountry() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByAddressCountry", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery()).isEqualTo("FROM Person WHERE addressCountry = ?1");
        assertThat(result.getParamCount()).isEqualTo(1);
    }

    @Test
    public void testFindAllByAddress_Country() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByAddress_Country", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery()).isEqualTo("FROM Person WHERE address.country = ?1");
        assertThat(result.getParamCount()).isEqualTo(1);
    }

    @Test
    public void testFindAllByAddressCountryIsoCode() throws Exception {
        UnableToParseMethodException exception = assertThrows(UnableToParseMethodException.class,
                () -> parseMethod(repositoryClass, "findAllByAddressCountryIsoCode", entityClass, additionalClasses));
        assertThat(exception).hasMessageContaining("Person does not contain a field named: addressCountryIsoCode");
    }

    @Test
    public void testFindAllByAddress_CountryIsoCode() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByAddress_CountryIsoCode", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery()).isEqualTo("FROM Person WHERE address.country.isoCode = ?1");
        assertThat(result.getParamCount()).isEqualTo(1);
    }

    @Test
    public void testFindAllByAddress_Country_IsoCode() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByAddress_Country_IsoCode", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery()).isEqualTo("FROM Person WHERE address.country.isoCode = ?1");
        assertThat(result.getParamCount()).isEqualTo(1);
    }

    @Test
    public void testFindAllByAddress_CountryInvalid() throws Exception {
        UnableToParseMethodException exception = assertThrows(UnableToParseMethodException.class,
                () -> parseMethod(repositoryClass, "findAllByAddress_CountryInvalid", entityClass, additionalClasses));
        assertThat(exception).hasMessageContaining("Person does not contain a field named: address_CountryInvalid");
        assertThat(exception).hasMessageContaining("Country.invalid");
    }

    @Test
    public void testFindAllBy_() throws Exception {
        UnableToParseMethodException exception = assertThrows(UnableToParseMethodException.class,
                () -> parseMethod(repositoryClass, "findAllBy_", entityClass, additionalClasses));
        assertThat(exception).hasMessageContaining("Person does not contain a field named: _");
    }

    private AbstractStringAssert<?> assertSameClass(ClassInfo classInfo, Class<?> aClass) {
        return assertThat(classInfo.name().toString()).isEqualTo(aClass.getName());
    }

    private MethodNameParser.Result parseMethod(Class<?> repositoryClass, String methodToParse,
            Class<?> entityClass, Class<?>... additionalClasses) throws IOException {
        IndexView indexView = index(ArrayUtils.addAll(additionalClasses, repositoryClass, entityClass));
        DotName repository = DotName.createSimple(repositoryClass.getName());
        DotName entity = DotName.createSimple(entityClass.getName());
        ClassInfo entityClassInfo = indexView.getClassByName(entity);
        ClassInfo repositoryClassInfo = indexView.getClassByName(repository);
        MethodNameParser methodNameParser = new MethodNameParser(entityClassInfo, indexView);
        MethodInfo repositoryMethod = repositoryClassInfo.firstMethod(methodToParse);
        MethodNameParser.Result result = methodNameParser.parse(repositoryMethod);
        return result;
    }

    public static Index index(Class<?>... classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (InputStream stream = MethodNameParserTest.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }
}
