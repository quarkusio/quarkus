package io.quarkus.it.hibernate.orm.cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.infinispan.quarkus.hibernate.cache.QuarkusInfinispanRegionFactory;

import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Basic test running JPA with the H2 database and Infinispan as second level cache provider.
 * The application can work in either standard JVM or in native mode, while we run H2 as a separate JVM process.
 */
@Path("/hibernate-orm-cache")
@ApplicationScoped
public class HibernateOrmCacheTestEndpoint {

    @Inject
    EntityManagerFactory emf;
    @Inject
    EntityManager em;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/testfunctionality")
    public String doGet() {
        doStuffWithHibernate();
        return "OK";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/memory-object-count/{region}")
    public String memoryObjectCount(@PathParam("region") String region) {
        QuarkusInfinispanRegionFactory regionFactory = getRegionFactory();
        Optional<Long> result = regionFactory.getMemoryObjectCount(region);
        return result
                .map(Object::toString)
                .orElseGet(
                        () -> String.format("Region %s not found", region));
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/expiration-max-idle/{region}")
    public String expirationMaxIdle(@PathParam("region") String region) {
        QuarkusInfinispanRegionFactory regionFactory = getRegionFactory();
        Optional<Duration> result = regionFactory.getExpirationMaxIdle(region);
        return result
                .map(duration -> Long.toString(duration.getSeconds()))
                .orElseGet(
                        () -> String.format("Region %s not found", region));
    }

    private QuarkusInfinispanRegionFactory getRegionFactory() {
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        CacheImplementor cache = (CacheImplementor) sessionFactory.getCache();
        return (QuarkusInfinispanRegionFactory) cache.getRegionFactory();
    }

    /**
     * Lists the various operations we want to test for:
     */
    private void doStuffWithHibernate() {
        //Cleanup any existing data:
        deleteAll();

        testReadOnly();
        testReadWrite();
        testNonStrictReadWrite();
        testQuery();

        testCollection();

        testReadOnlyNaturalId();
        testReadWriteNaturalId();

        //Delete all
        testDeleteViaRemove();
        testDeleteViaQuery();
    }

    private void testReadOnlyNaturalId() {
        Map<String, Counts> counts = new TreeMap<>();
        counts.put(Country.class.getName(), new Counts(3, 0, 0, 3));
        counts.put(Country.class.getName() + "##NaturalId", new Counts(3, 0, 0, 3));
        storeTestCountries(counts);

        counts = new TreeMap<>();
        counts.put(Country.class.getName(), new Counts(0, 1, 0, 3));
        counts.put(Country.class.getName() + "##NaturalId", new Counts(0, 1, 0, 3));
        verifyFindCountryByNaturalId("+41", "Switzerland");
        assertRegionStats(counts);
    }

    private void storeTestCountries(Map<String, Counts> counts) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            em.persist(new Country("Spain", "+34"));
            em.persist(new Country("Switzerland", "+41"));
            em.persist(new Country("France", "+33"));
        });

        assertRegionStats(counts);
    }

    private void testReadWriteNaturalId() {
        Map<String, Counts> counts = new TreeMap<>();
        counts.put(Citizen.class.getName(), new Counts(3, 0, 0, 3));
        counts.put(Citizen.class.getName() + "##NaturalId", new Counts(3, 0, 0, 3));
        storeTestCitizens(counts);

        counts = new TreeMap<>();
        counts.put(Citizen.class.getName(), new Counts(0, 1, 0, 3));
        counts.put(Citizen.class.getName() + "##NaturalId", new Counts(0, 1, 0, 3));
        verifyFindCitizenByNaturalId("96246496Y", "Snow", counts);

        counts = new TreeMap<>();
        counts.put(Citizen.class.getName(), new Counts(1, 1, 0, 3));
        counts.put(Citizen.class.getName() + "##NaturalId", new Counts(1, 1, 0, 3));
        updateNaturalId(counts);

        counts = new TreeMap<>();
        counts.put(Citizen.class.getName(), new Counts(0, 1, 0, 3));
        counts.put(Citizen.class.getName() + "##NaturalId", new Counts(0, 1, 0, 3));
        verifyFindCitizenByNaturalId("78902007R", "Stark", counts);
    }

    private void verifyFindCountryByNaturalId(String callingCode, String expectedName) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            final Session session = em.unwrap(Session.class);
            final NaturalIdLoadAccess<Country> loader = session.byNaturalId(Country.class);
            loader.using("callingCode", callingCode);
            Country country = loader.load();
            if (!country.getName().equals(expectedName))
                throw new RuntimeException("Incorrect citizen: " + country.getName() + ", expected: " + expectedName);
        });
    }

    private void updateNaturalId(Map<String, Counts> counts) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            final Session session = em.unwrap(Session.class);
            final NaturalIdLoadAccess<Citizen> loader = session.byNaturalId(Citizen.class);
            loader.using("ssn", "45989213T");
            Citizen citizen = loader.load();
            String expected = "Stark";
            if (!citizen.getLastname().equals(expected))
                throw new RuntimeException("Incorrect citizen: " + citizen.getLastname() + ", expected: " + expected);

            citizen.setSsn("78902007R");
        });

        assertRegionStats(counts);
    }

    private void verifyFindCitizenByNaturalId(String ssn, String expectedLastName,
            Map<String, Counts> counts) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            final Session session = em.unwrap(Session.class);
            final NaturalIdLoadAccess<Citizen> loader = session.byNaturalId(Citizen.class);
            loader.using("ssn", ssn);
            Citizen citizen = loader.load();
            if (!citizen.getLastname().equals(expectedLastName))
                throw new RuntimeException("Incorrect citizen: " + citizen.getLastname() + ", expected: " + expectedLastName);
        });

        assertRegionStats(counts);
    }

    private void storeTestCitizens(Map<String, Counts> counts) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            em.persist(new Citizen("Aria", "Stark", "45989213T"));
            em.persist(new Citizen("Jon", "Snow", "96246496Y"));
            em.persist(new Citizen("Tyrion", "Lannister", "09287101T"));
        });

        assertRegionStats(counts);
    }

    private void testCollection() {
        // Collections not stored on cache on inserts
        Map<String, Counts> counts = new TreeMap<>();
        counts.put(Trainer.class.getName(), new Counts(1, 0, 0, 1));
        counts.put(Trainer.class.getName() + ".pokemons", new Counts(0, 0, 0, 0));
        storeTestPokemonTrainers(counts);

        counts = new TreeMap<>();
        counts.put(Trainer.class.getName(), new Counts(0, 1, 0, 1));
        counts.put(Trainer.class.getName() + ".pokemons", new Counts(1, 0, 1, 1));
        verifyReadWriteCollection(3, counts);

        counts = new TreeMap<>();
        counts.put(Trainer.class.getName(), new Counts(0, 1, 0, 1));
        // Collections get evicted upon updates
        counts.put(Trainer.class.getName() + ".pokemons", new Counts(0, 1, 0, 0));
        addTestPokemonForTrainer(counts);

        counts = new TreeMap<>();
        counts.put(Trainer.class.getName(), new Counts(0, 1, 0, 1));
        counts.put(Trainer.class.getName() + ".pokemons", new Counts(1, 0, 1, 1));
        verifyReadWriteCollection(4, counts);
    }

    private void storeTestPokemonTrainers(Map<String, Counts> counts) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            final Pokemon rocky = new Pokemon(68, "Rocky", 3056);
            final Pokemon sonGoku = new Pokemon(149, "Son Goku", 3792);
            final Pokemon mmMan = new Pokemon(94, "Marshmallow Man", 2842);
            em.persist(rocky);
            em.persist(sonGoku);
            em.persist(mmMan);
            em.persist(new Trainer(rocky, sonGoku, mmMan));
        });

        assertRegionStats(counts);
    }

    private void addTestPokemonForTrainer(Map<String, Counts> counts) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            final Trainer t1 = em.find(Trainer.class, 1L);
            final List<Pokemon> pokemons = t1.getPokemons();

            final Pokemon golem = new Pokemon(76, "Alolan Golem", 2233);
            em.persist(golem);
            pokemons.add(golem);
        });

        assertRegionStats(counts);
    }

    private void verifyReadWriteCollection(int expectedSize,
            Map<String, Counts> counts) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            final Trainer t1 = em.find(Trainer.class, 1L);
            final List<Pokemon> pokemons = t1.getPokemons();

            if (pokemons.size() != expectedSize)
                throw new RuntimeException("Incorrect family size: " + pokemons.size() + ", expected: " + expectedSize);
        });

        assertRegionStats(counts);
    }

    private void testNonStrictReadWrite() {
        //Store some well known Item instances we can then test on:
        storeTestItems(new Counts(3, 0, 0, 3));

        //Load all items and run some checks on the cache hits
        final String[] expected = { "Hibernate T-shirt", "Hibernate Sticker", "Hibernate Mug" };
        verifyFindByIdItems(expected, new Counts(0, 3, 0, 3));

        //Modify item descriptions
        final String[] newValues = { "Infinispan T-shirt", "Infinispan Sticker", "Infinispan Mug" };
        updateItemDescriptions(newValues, new Counts(3, 3, 0, 3));

        //Verify descriptions after update
        verifyFindByIdItems(newValues, new Counts(0, 3, 0, 3));
    }

    private void updateItemDescriptions(String[] newValues, Counts expected) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            final Item i1 = em.find(Item.class, 1L);
            i1.setDescription(newValues[0]);
            final Item i2 = em.find(Item.class, 2L);
            i2.setDescription(newValues[1]);
            final Item i3 = em.find(Item.class, 3L);
            i3.setDescription(newValues[2]);
        });

        assertRegionStats(expected, Item.class.getName());
    }

    private void storeTestItems(Counts expected) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            final Item tshirt = new Item("tshirt", "Hibernate T-shirt");
            em.persist(tshirt);
            final Item sticker = new Item("sticker", "Hibernate Sticker");
            em.persist(sticker);
            final Item mug = new Item("mug", "Hibernate Mug");
            em.persist(mug);
        });

        assertRegionStats(expected, Item.class.getName());
    }

    private void verifyFindByIdItems(String[] expectedDesc, Counts expected) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            findByIdItems(em, expectedDesc);
        });

        assertRegionStats(expected, Item.class.getName());
    }

    private static void findByIdItems(EntityManager em, String[] expectedDesc) {
        final Item i1 = em.find(Item.class, 1L);
        if (!i1.getDescription().equals(expectedDesc[0]))
            throw new RuntimeException("Incorrect description: " + i1.getDescription() + ", expected: " + expectedDesc[0]);

        final Item i2 = em.find(Item.class, 2L);
        if (!i2.getDescription().equals(expectedDesc[1]))
            throw new RuntimeException("Incorrect description: " + i2.getDescription() + ", expected: " + expectedDesc[1]);

        final Item i3 = em.find(Item.class, 3L);
        if (!i3.getDescription().equals(expectedDesc[2]))
            throw new RuntimeException("Incorrect description: " + i3.getDescription() + ", expected: " + expectedDesc[2]);

        List<Item> allitems = Arrays.asList(i1, i2, i3);
        if (allitems.size() != 3) {
            throw new RuntimeException("Incorrect number of results");
        }
        StringBuilder sb = new StringBuilder("list of stored Items names:\n\t");
        for (Item p : allitems)
            p.describeFully(sb);

        sb.append("\nList complete.\n");
        System.out.print(sb);
    }

    private void testQuery() {
        //Load all persons and run some checks on the query results:
        Map<String, Counts> counts = new TreeMap<>();
        counts.put(Person.class.getName(), new Counts(4, 0, 0, 4));
        counts.put(RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME, new Counts(1, 0, 1, 1));
        verifyListOfExistingPersons(counts);

        //Load all persons with same query and verify query results
        counts = new TreeMap<>();
        //(!) There is a semantic difference here between ORM5 and ORM6: see
        // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#query-result-cache
        counts.put(Person.class.getName(), new Counts(0, 0, 0, 4));
        counts.put(RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME, new Counts(0, 1, 0, 1));
        verifyListOfExistingPersons(counts);
    }

    private void testReadOnly() {
        //Store some well known Person instances we can then test on:
        storeTestPersons(new Counts(4, 0, 0, 4));

        //Load all persons and run some checks on the cache hits
        verifyFindByIdPersons();
        assertRegionStats(new Counts(0, 4, 0, 4), Person.class.getName());
    }

    private void verifyFindByIdPersons() {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            findByIdPersons(em);
        });
    }

    private static void findByIdPersons(EntityManager em) {
        final Person p1 = em.find(Person.class, 1L);
        final Person p2 = em.find(Person.class, 2L);
        final Person p3 = em.find(Person.class, 3L);
        final Person p4 = em.find(Person.class, 4L);
        List<Person> allpersons = Arrays.asList(p1, p2, p3, p4);
        if (allpersons.size() != 4) {
            throw new RuntimeException("Incorrect number of results");
        }
        StringBuilder sb = new StringBuilder("list of stored Person names:\n\t");
        for (Person p : allpersons) {
            p.describeFully(sb);
        }
        sb.append("\nList complete.\n");
        System.out.print(sb);
    }

    private void verifyListOfExistingPersons(Map<String, Counts> counts) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            listExistingPersons(em);
        });

        assertRegionStats(counts);
    }

    private void storeTestPersons(Counts expected) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            em.persist(new Person("Gizmo"));
            em.persist(new Person("Quarkus"));
            em.persist(new Person("Hibernate ORM"));
            em.persist(new Person("Infinispan"));
        });

        assertRegionStats(expected, Person.class.getName());
    }

    private void deleteAll() {
        QuarkusTransaction.requiringNew().run(() -> {
            em.createNativeQuery("Delete from Person").executeUpdate();
            em.createNativeQuery("Delete from Item").executeUpdate();
            em.createNativeQuery("Delete from Citizen").executeUpdate();
            em.createNativeQuery("Delete from Country").executeUpdate();
            em.createNativeQuery("Delete from Pokemon").executeUpdate();
            em.createNativeQuery("Delete from Trainer").executeUpdate();
        });
    }

    private void testDeleteViaQuery() {
        QuarkusTransaction.requiringNew().run(() -> {
            em.createNativeQuery("Delete from Person").executeUpdate();
        });

        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            if (em.find(Person.class, 1L) != null
                    || em.find(Person.class, 2L) != null
                    || em.find(Person.class, 3L) != null
                    || em.find(Person.class, 4L) != null) {
                throw new RuntimeException("Persons should have been deleted");
            }

        });

        assertRegionStats(new Counts(0, 0, 4, 0), Person.class.

                getName());
    }

    private void testDeleteViaRemove() {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            em.remove(em.find(Pokemon.class, 3));
            em.remove(em.find(Pokemon.class, 248));
            em.remove(em.find(Pokemon.class, 242));
        });

        assertRegionStats(new Counts(0, 3, 0, 4), Pokemon.class.getName());

        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            if (em.find(Pokemon.class, 3) != null
                    || em.find(Pokemon.class, 248) != null
                    || em.find(Pokemon.class, 242) != null) {
                throw new RuntimeException("Pokemons should have been deleted");
            }
        });

        assertRegionStats(new Counts(0, 0, 3, 4), Pokemon.class.getName());
    }

    private static void listExistingPersons(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Person> cq = cb.createQuery(Person.class);
        Root<Person> from = cq.from(Person.class);
        cq.select(from).orderBy(cb.asc(from.get("name")));
        TypedQuery<Person> q = em.createQuery(cq);
        q.setHint("org.hibernate.cacheable", Boolean.TRUE);
        List<Person> allpersons = q.getResultList();
        if (allpersons.size() != 4) {
            throw new RuntimeException("Incorrect number of results");
        }
        if (!allpersons.get(0).getName().equals("Gizmo")) {
            throw new RuntimeException("Incorrect order of results");
        }
        StringBuilder sb = new StringBuilder("list of stored Person names:\n\t");
        for (Person p : allpersons) {
            p.describeFully(sb);
        }
        sb.append("\nList complete.\n");
        System.out.print(sb);
    }

    private void testReadWrite() {
        //Store some well known Pokemon instances we can then test on:
        storeTestPokemons(new Counts(3, 0, 0, 3));

        //Load all persons and run some checks on the cache hits
        verifyFindByIdPokemons(new int[] { 2555, 3670, 3219 }, new Counts(0, 3, 0, 3));

        //Rebalance cp values for pokemons
        rebalanceCpsForPokemons(new Counts(3, 3, 0, 3));

        //Verify cp values after update
        verifyFindByIdPokemons(new int[] { 2707, 3834, 2757 }, new Counts(0, 3, 0, 3));
    }

    private void rebalanceCpsForPokemons(Counts expected) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            Pokemon igeldo = em.find(Pokemon.class, 3);
            igeldo.setCp(2707);
            Pokemon godzilla = em.find(Pokemon.class, 248);
            godzilla.setCp(3834);
            Pokemon blissey = em.find(Pokemon.class, 242);
            blissey.setCp(2757);

        });

        assertRegionStats(expected, Pokemon.class.getName());
    }

    private void verifyFindByIdPokemons(int[] expectedCps, Counts expected) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            Pokemon igeldo = em.find(Pokemon.class, 3);
            if (igeldo.getCp() != expectedCps[0])
                throw new RuntimeException("Incorrect cp: " + igeldo.getCp());

            Pokemon godzilla = em.find(Pokemon.class, 248);
            if (godzilla.getCp() != expectedCps[1])
                throw new RuntimeException("Incorrect cp: " + godzilla.getCp());

            Pokemon blissey = em.find(Pokemon.class, 242);
            if (blissey.getCp() != expectedCps[2])
                throw new RuntimeException("Incorrect cp: " + blissey.getCp());

        });

        assertRegionStats(expected, Pokemon.class.getName());
    }

    private void storeTestPokemons(Counts expected) {
        clearStatistics();

        QuarkusTransaction.requiringNew().run(() -> {
            final Pokemon igeldo = new Pokemon(3, "Venusaur", 2555);
            em.persist(igeldo);
            final Pokemon godzilla = new Pokemon(248, "Tyranitar", 3670);
            em.persist(godzilla);
            final Pokemon khaleesi = new Pokemon(242, "Blissey", 3219);
            em.persist(khaleesi);

        });

        assertRegionStats(expected, Pokemon.class.getName());
    }

    private void clearStatistics() {
        emf.unwrap(SessionFactory.class).getStatistics().clear();
    }

    private Statistics getStatistics() {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }

    private void assertRegionStats(Map<String, Counts> counts) {
        Statistics stats = getStatistics();
        for (Map.Entry<String, Counts> entry : counts.entrySet()) {
            final String region = entry.getKey();
            final Counts expected = entry.getValue();
            final Counts actual = statsToCounts(region, stats);
            assertCountEquals(expected, actual, region);
        }
    }

    private void assertRegionStats(Counts expected, String region) {
        Statistics stats = getStatistics();
        final Counts actual = statsToCounts(region, stats);
        assertCountEquals(expected, actual, region);
    }

    private Counts statsToCounts(String region, Statistics stats) {
        final CacheRegionStatistics cacheStats = stats.getDomainDataRegionStatistics(region);
        return new Counts(
                cacheStats.getPutCount(), cacheStats.getHitCount(), cacheStats.getMissCount(),
                cacheStats.getElementCountInMemory());
    }

    private void assertCountEquals(Counts expected, Counts actual, String msg) {
        if (!expected.equals(actual))
            throw unequalCounts(expected, msg, actual);
    }

    private RuntimeException unequalCounts(Counts expected, String region, Counts actual) {
        return new RuntimeException(
                "[" + region + "] expected " + expected + " second level cache count, instead got: " + actual);
    }

    static final class Counts {
        final long puts;
        final long hits;
        final long misses;
        final long numElements;

        Counts(long puts, long hits, long misses, long numElements) {
            this.puts = puts;
            this.hits = hits;
            this.misses = misses;
            this.numElements = numElements;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Counts counts = (Counts) o;
            return puts == counts.puts &&
                    hits == counts.hits &&
                    misses == counts.misses &&
                    numElements == counts.numElements;
        }

        @Override
        public int hashCode() {
            return Objects.hash(puts, hits, misses, numElements);
        }

        @Override
        public String toString() {
            return "Counts{" +
                    "puts=" + puts +
                    ", hits=" + hits +
                    ", misses=" + misses +
                    ", numElements=" + numElements +
                    '}';
        }
    }

}
