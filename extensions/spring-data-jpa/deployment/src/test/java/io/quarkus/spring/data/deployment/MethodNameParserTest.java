package io.quarkus.spring.data.deployment;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;
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

import io.quarkus.spring.data.deployment.nested.fields.generics.ChildBase;
import io.quarkus.spring.data.deployment.nested.fields.generics.ParentBase;
import io.quarkus.spring.data.deployment.nested.fields.generics.ParentBaseRepository;

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
        assertThat(result.getParamCount()).isEqualTo(1);
        assertThat(result.getQuery())
                .isEqualTo("SELECT person FROM Person AS person LEFT JOIN person.address address WHERE address.zipCode = ?1");
    }

    @Test
    public void testFindAllByNameAndOrder() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByNameAndOrder", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getParamCount()).isEqualTo(2);
        assertThat(result.getQuery())
                .isEqualTo("SELECT person FROM Person AS person WHERE name = ?1 AND order = ?2");
    }

    @Test
    public void testFindAllByNameOrOrder() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByNameOrOrder", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getParamCount()).isEqualTo(2);
        assertThat(result.getQuery())
                .isEqualTo("SELECT person FROM Person AS person WHERE name = ?1 OR order = ?2");
    }

    @Test
    public void testFindAllByAddressCountry() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByAddressCountry", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery()).isEqualTo("SELECT person FROM Person AS person WHERE addressCountry = ?1");
        assertThat(result.getParamCount()).isEqualTo(1);
    }

    @Test
    public void findAllByNameOrAgeOrActive() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByNameOrAgeOrActive", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery())
                .isEqualTo("SELECT person FROM Person AS person WHERE name = ?1 OR age = ?2 OR active = ?3");
        assertThat(result.getParamCount()).isEqualTo(3);
    }

    @Test
    public void findAllByNameAndAgeOrActive() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByNameAndAgeOrActive", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery())
                .isEqualTo("SELECT person FROM Person AS person WHERE name = ?1 AND age = ?2 OR active = ?3");
        assertThat(result.getParamCount()).isEqualTo(3);
    }

    @Test
    public void findAllByNameAndAgeAndActive() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByNameAndAgeAndActive", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery())
                .isEqualTo("SELECT person FROM Person AS person WHERE name = ?1 AND age = ?2 AND active = ?3");
        assertThat(result.getParamCount()).isEqualTo(3);
    }

    @Test
    public void testFindAllByAddress_Country() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByAddress_Country", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery())
                .isEqualTo("SELECT person FROM Person AS person LEFT JOIN person.address address WHERE address.country = ?1");
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
        assertThat(result.getQuery())
                .isEqualTo(
                        "SELECT person FROM Person AS person LEFT JOIN person.address address WHERE address.country.isoCode = ?1");
        assertThat(result.getParamCount()).isEqualTo(1);
    }

    @Test
    public void testFindAllByAddress_Country_IsoCode() throws Exception {
        MethodNameParser.Result result = parseMethod(repositoryClass, "findAllByAddress_Country_IsoCode", entityClass,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), entityClass);
        assertThat(result.getQuery())
                .isEqualTo(
                        "SELECT person FROM Person AS person LEFT JOIN person.address address WHERE address.country.isoCode = ?1");
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

    @Test
    public void testGenericsWithWildcard() throws Exception {
        Class[] additionalClasses = new Class[] { ChildBase.class };

        MethodNameParser.Result result = parseMethod(ParentBaseRepository.class, "countParentsByChildren_Nombre",
                ParentBase.class,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), ParentBase.class);
        assertThat(result.getQuery())
                .isEqualTo(
                        "FROM ParentBase AS parentbase LEFT JOIN parentbase.children children WHERE children.nombre = ?1");
        assertThat(result.getParamCount()).isEqualTo(1);
    }

    @Test
    public void shouldParseRepositoryMethodOverEntityContainingACollection() throws Exception {
        Class[] additionalClasses = new Class[] { LoginEvent.class };

        MethodNameParser.Result result = parseMethod(UserRepository.class, "countUsersByLoginEvents_Id",
                User.class,
                additionalClasses);
        assertThat(result).isNotNull();
        assertSameClass(result.getEntityClass(), User.class);
        assertThat(result.getParamCount()).isEqualTo(1);
        assertThat(result.getQuery()).isEqualTo(
                "FROM User AS user LEFT JOIN loginEvents loginEvents ON user.userId = loginEvents.user.userId WHERE loginEvents.id = ?1");
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
                    .getResourceAsStream(fromClassNameToResourceName(clazz.getName()))) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }
}
