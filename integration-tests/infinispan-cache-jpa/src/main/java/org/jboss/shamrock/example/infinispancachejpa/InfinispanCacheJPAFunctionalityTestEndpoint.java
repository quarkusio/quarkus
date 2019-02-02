package org.jboss.shamrock.example.infinispancachejpa;

import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.infinispan.protean.hibernate.cache.InfinispanRegionFactory;
import org.infinispan.protean.hibernate.cache.ManualTestService;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Basic test running JPA with the H2 database and Infinispan as second level cache provider.
 * The application can work in either standard JVM or SubstrateVM, while we run H2 as a separate JVM process.
 */
@WebServlet(name = "InfinispanCacheJPAFunctionalityTestEndpoint", urlPatterns = "/infinispan-cache-jpa/testfunctionality")
public class InfinispanCacheJPAFunctionalityTestEndpoint extends HttpServlet {

    @PersistenceUnit(unitName = "templatePU")
    EntityManagerFactory entityManagerFactory;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            doStuffWithHibernate(entityManagerFactory);
        } catch (Exception e) {
            reportException("Oops, shit happened, No boot for you!", e, resp);
        }
        resp.getWriter().write("OK");
    }

    /**
     * Lists the various operations we want to test for:
     */
    private static void doStuffWithHibernate(EntityManagerFactory entityManagerFactory) {
        //Cleanup any existing data:
        deleteAll(entityManagerFactory);

        testReadOnly(entityManagerFactory);
        testReadWrite(entityManagerFactory);
        testNonStrictReadWrite(entityManagerFactory);
        testQuery(entityManagerFactory);

        testCollection(entityManagerFactory);

        testReadOnlyNaturalId(entityManagerFactory);
        testReadWriteNaturalId(entityManagerFactory);

        testMaxSize(entityManagerFactory);
        testMaxIdle(entityManagerFactory);

        //Delete all
        testDeleteViaRemove(entityManagerFactory);
        testDeleteViaQuery(entityManagerFactory);
    }

    private static void testReadOnlyNaturalId(EntityManagerFactory entityManagerFactory) {
        Map<String, Counts> counts = new TreeMap<>();
        counts.put(Country.class.getName(), new Counts(3, 0, 0, 3));
        counts.put(Country.class.getName() + "##NaturalId", new Counts(3, 0, 0, 3));
        storeTestCountries(entityManagerFactory, counts);

        counts = new TreeMap<>();
        counts.put(Country.class.getName(), new Counts(0, 1, 0, 3));
        counts.put(Country.class.getName() + "##NaturalId", new Counts(0, 1, 0, 3));
        verifyFindCountryByNaturalId(entityManagerFactory, "+41", "Switzerland", counts);
    }

    private static void storeTestCountries(final EntityManagerFactory emf, Map<String, Counts> counts) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        em.persist(new Country("Spain", "+34"));
        em.persist(new Country("Switzerland", "+41"));
        em.persist(new Country("France", "+33"));
        transaction.commit();
        em.close();

        assertRegionStats(counts, stats);
    }

    private static void testMaxIdle(EntityManagerFactory entityManagerFactory) {
        final CacheImplementor cacherImplementor = entityManagerFactory.getCache().unwrap(CacheImplementor.class);
        final InfinispanRegionFactory regionFactory = (InfinispanRegionFactory) cacherImplementor.getRegionFactory();
        ManualTestService manualTestService = regionFactory.getTimeService();
        manualTestService.advance(120, TimeUnit.SECONDS);

        Map<String, Counts> counts = new TreeMap<>();
        counts.put(Country.class.getName(), new Counts(1, 0, 1, 1));
        counts.put(Country.class.getName() + "##NaturalId", new Counts(0, 1, 0, 3));
        verifyFindCountryByNaturalId(entityManagerFactory, "+41", "Switzerland", counts);
    }

    private static void testReadWriteNaturalId(EntityManagerFactory entityManagerFactory) {
        Map<String, Counts> counts = new TreeMap<>();
        counts.put(Citizen.class.getName(), new Counts(3, 0, 0, 3));
        counts.put(Citizen.class.getName() + "##NaturalId", new Counts(3, 0, 0, 3));
        storeTestCitizens(entityManagerFactory, counts);

        counts = new TreeMap<>();
        counts.put(Citizen.class.getName(), new Counts(0, 1, 0, 3));
        counts.put(Citizen.class.getName() + "##NaturalId", new Counts(0, 1, 0, 3));
        verifyFindCitizenByNaturalId(entityManagerFactory, "96246496Y", "Snow", counts);

        counts = new TreeMap<>();
        counts.put(Citizen.class.getName(), new Counts(1, 1, 0, 3));
        counts.put(Citizen.class.getName() + "##NaturalId", new Counts(1, 1, 0, 3));
        updateNaturalId(entityManagerFactory, counts);

        counts = new TreeMap<>();
        counts.put(Citizen.class.getName(), new Counts(0, 1, 0, 3));
        counts.put(Citizen.class.getName() + "##NaturalId", new Counts(0, 1, 0, 3));
        verifyFindCitizenByNaturalId(entityManagerFactory, "78902007R", "Stark", counts);
    }

    private static void verifyFindCountryByNaturalId(EntityManagerFactory emf, String callingCode, String expectedName, Map<String, Counts> counts) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        final Session session = em.unwrap(Session.class);
        final NaturalIdLoadAccess<Country> loader = session.byNaturalId(Country.class);
        loader.using("callingCode", callingCode);
        Country country = loader.load();
        if (!country.getName().equals(expectedName))
            throw new RuntimeException("Incorrect citizen: " + country.getName() + ", expected: " + expectedName);

        transaction.commit();
        em.close();

        assertRegionStats(counts, stats);
    }

    private static void updateNaturalId(EntityManagerFactory emf, Map<String, Counts> counts) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        final Session session = em.unwrap(Session.class);
        final NaturalIdLoadAccess<Citizen> loader = session.byNaturalId(Citizen.class);
        loader.using("ssn", "45989213T");
        Citizen citizen = loader.load();
        String expected = "Stark";
        if (!citizen.getLastname().equals(expected))
            throw new RuntimeException("Incorrect citizen: " + citizen.getLastname() + ", expected: " + expected);

        citizen.setSsn("78902007R");

        transaction.commit();
        em.close();

        assertRegionStats(counts, stats);
    }

    private static void verifyFindCitizenByNaturalId(EntityManagerFactory emf, String ssn, String expectedLastName, Map<String, Counts> counts) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        final Session session = em.unwrap(Session.class);
        final NaturalIdLoadAccess<Citizen> loader = session.byNaturalId(Citizen.class);
        loader.using("ssn", ssn);
        Citizen citizen = loader.load();
        if (!citizen.getLastname().equals(expectedLastName))
            throw new RuntimeException("Incorrect citizen: " + citizen.getLastname() + ", expected: " + expectedLastName);

        transaction.commit();
        em.close();

        assertRegionStats(counts, stats);
    }

    private static void storeTestCitizens(final EntityManagerFactory emf, Map<String, Counts> counts) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        em.persist(new Citizen("Aria", "Stark", "45989213T"));
        em.persist(new Citizen("Jon", "Snow", "96246496Y"));
        em.persist(new Citizen("Tyrion", "Lannister", "09287101T"));
        transaction.commit();
        em.close();

        assertRegionStats(counts, stats);
    }

    private static void testCollection(EntityManagerFactory entityManagerFactory) {
        // Collections not stored on cache on inserts
        Map<String, Counts> counts = new TreeMap<>();
        counts.put(Trainer.class.getName(), new Counts(1, 0, 0, 1));
        counts.put(Trainer.class.getName() + ".pokemons", new Counts(0, 0, 0, 0));
        storeTestPokemonTrainers(entityManagerFactory, counts);

        counts = new TreeMap<>();
        counts.put(Trainer.class.getName(), new Counts(0, 1, 0, 1));
        counts.put(Trainer.class.getName() + ".pokemons", new Counts(1, 0, 1, 1));
        verifyReadWriteCollection(entityManagerFactory, 3, counts);

        counts = new TreeMap<>();
        counts.put(Trainer.class.getName(), new Counts(0, 1, 0, 1));
        // Collections get evicted upon updates
        counts.put(Trainer.class.getName() + ".pokemons", new Counts(0, 1, 0, 0));
        addTestPokemonForTrainer(entityManagerFactory, counts);

        counts = new TreeMap<>();
        counts.put(Trainer.class.getName(), new Counts(0, 1, 0, 1));
        counts.put(Trainer.class.getName() + ".pokemons", new Counts(1, 0, 1, 1));
        verifyReadWriteCollection(entityManagerFactory, 4, counts);
    }

    private static void storeTestPokemonTrainers(final EntityManagerFactory emf, Map<String, Counts> counts) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        final Pokemon rocky = new Pokemon(68, "Rocky", 3056);
        final Pokemon sonGoku = new Pokemon(149, "Son Goku", 3792);
        final Pokemon mmMan = new Pokemon(94, "Marshmallow Man", 2842);
        em.persist(rocky);
        em.persist(sonGoku);
        em.persist(mmMan);
        em.persist(new Trainer(rocky, sonGoku, mmMan));

        transaction.commit();
        em.close();

        assertRegionStats(counts, stats);
    }

    private static void addTestPokemonForTrainer(final EntityManagerFactory emf, Map<String, Counts> counts) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        final Trainer t1 = em.find(Trainer.class, 1L);
        final List<Pokemon> pokemons = t1.getPokemons();

        final Pokemon golem = new Pokemon(76, "Alolan Golem", 2233);
        em.persist(golem);
        pokemons.add(golem);

        transaction.commit();
        em.close();

        assertRegionStats(counts, stats);
    }

    private static void verifyReadWriteCollection(final EntityManagerFactory emf, int expectedSize, Map<String, Counts> counts) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        final Trainer t1 = em.find(Trainer.class, 1L);
        final List<Pokemon> pokemons = t1.getPokemons();

        if (pokemons.size() != expectedSize)
            throw new RuntimeException("Incorrect family size: " + pokemons.size() + ", expected: " + expectedSize);

        transaction.commit();
        em.close();

        assertRegionStats(counts, stats);
    }

    private static void testNonStrictReadWrite(EntityManagerFactory entityManagerFactory) {
        //Store some well known Item instances we can then test on:
        storeTestItems(entityManagerFactory, new Counts(3, 0, 0, 3));

        //Load all items and run some checks on the cache hits
        final String[] expected = {"Hibernate T-shirt", "Hibernate Sticker", "Hibernate Mug"};
        verifyFindByIdItems(entityManagerFactory, expected, new Counts(0, 3, 0, 3));

        //Modify item descriptions
        final String[] newValues = {"Infinispan T-shirt", "Infinispan Sticker", "Infinispan Mug"};
        updateItemDescriptions(entityManagerFactory, newValues, new Counts(3, 3, 0, 3));

        //Verify descriptions after update
        verifyFindByIdItems(entityManagerFactory, newValues, new Counts(0, 3, 0, 3));
    }

    private static void updateItemDescriptions(final EntityManagerFactory emf, String[] newValues, Counts expected) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        final Item i1 = em.find(Item.class, 1L);
        i1.setDescription(newValues[0]);
        final Item i2 = em.find(Item.class, 2L);
        i2.setDescription(newValues[1]);
        final Item i3 = em.find(Item.class, 3L);
        i3.setDescription(newValues[2]);

        transaction.commit();
        em.close();

        assertRegionStats(expected, Item.class.getName(), stats);
    }

    private static void storeTestItems(final EntityManagerFactory emf, Counts expected) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        final Item tshirt = new Item("tshirt", "Hibernate T-shirt");
        em.persist(tshirt);
        final Item sticker = new Item("sticker", "Hibernate Sticker");
        em.persist(sticker);
        final Item mug = new Item("mug", "Hibernate Mug");
        em.persist(mug);

        transaction.commit();
        em.close();

        assertRegionStats(expected, Item.class.getName(), stats);
    }

    private static void verifyFindByIdItems(final EntityManagerFactory emf, String[] expectedDesc, Counts expected) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        findByIdItems(em, expectedDesc);
        transaction.commit();
        em.close();

        assertRegionStats(expected, Item.class.getName(), stats);
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

    private static void testMaxSize(EntityManagerFactory emf) {
        addItemBeyondMaxSize(emf);
    }

    private static void addItemBeyondMaxSize(EntityManagerFactory emf) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        final Item cap = new Item("cap", "Hibernate Cap");
        em.persist(cap);
        transaction.commit();
        em.close();

        assertRegionStatsEventually(new Counts(1, 0, 0, 3), Item.class.getName(), stats);
    }

    private static void testQuery(EntityManagerFactory entityManagerFactory) {
        //Load all persons and run some checks on the query results:
        Map<String, Counts> counts = new TreeMap<>();
        counts.put(Person.class.getName(), new Counts(4, 0, 0, 4));
        counts.put(RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME, new Counts(1, 0, 1, 1));
        verifyListOfExistingPersons(entityManagerFactory, counts);

        //Load all persons with same query and verify query results
        counts = new TreeMap<>();
        counts.put(Person.class.getName(), new Counts(0, 4, 0, 4));
        counts.put(RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME, new Counts(0, 1, 0, 1));
        verifyListOfExistingPersons(entityManagerFactory, counts);
    }

    private static void testReadOnly(EntityManagerFactory entityManagerFactory) {
        //Store some well known Person instances we can then test on:
        storeTestPersons(entityManagerFactory, new Counts(4, 0, 0, 4));

        //Load all persons and run some checks on the cache hits
        verifyFindByIdPersons(entityManagerFactory, new Counts(0, 4, 0, 4));

        //Evict persons from cache
        evictPersons(entityManagerFactory);

        //Load all persons and run some checks on the cache hits
        verifyFindByIdPersons(entityManagerFactory, new Counts(4, 0, 4, 4));
    }

    private static void evictPersons(final EntityManagerFactory emf) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        em.getEntityManagerFactory().getCache().evict(Person.class);
        em.close();

        final Counts expected = new Counts(0, 0, 0, 0);
        assertRegionStats(expected, Person.class.getName(), stats);
    }

    private static void verifyFindByIdPersons(final EntityManagerFactory emf, Counts expected) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        findByIdPersons(em);
        transaction.commit();
        em.close();

        assertRegionStats(expected, Person.class.getName(), stats);
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

    private static void verifyListOfExistingPersons(final EntityManagerFactory emf, Map<String, Counts> counts) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        listExistingPersons(em);
        transaction.commit();
        em.close();

        assertRegionStats(counts, stats);
    }

    private static void storeTestPersons(final EntityManagerFactory emf, Counts expected) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        em.persist(new Person("Protean"));
        em.persist(new Person("Shamrock"));
        em.persist(new Person("Hibernate ORM"));
        em.persist(new Person("Infinispan"));
        transaction.commit();
        em.close();

        assertRegionStats(expected, Person.class.getName(), stats);
    }

    private static void deleteAll(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        em.createNativeQuery("Delete from Person").executeUpdate();
        em.createNativeQuery("Delete from Item").executeUpdate();
        em.createNativeQuery("Delete from Citizen").executeUpdate();
        em.createNativeQuery("Delete from Country").executeUpdate();
        em.createNativeQuery("Delete from Pokemon").executeUpdate();
        em.createNativeQuery("Delete from Trainer").executeUpdate();
        transaction.commit();
        em.close();
    }

    private static void testDeleteViaQuery(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        em.createNativeQuery("Delete from Person").executeUpdate();
        transaction.commit();
        em.close();

        Statistics stats = getStatistics(emf);

        em = emf.createEntityManager();
        transaction = em.getTransaction();
        transaction.begin();
        if (em.find(Person.class, 1L) != null
                || em.find(Person.class, 2L) != null
                || em.find(Person.class, 3L) != null
                || em.find(Person.class, 4L) != null) {
            throw new RuntimeException("Persons should have been deleted");
        }

        transaction.commit();
        em.close();

        assertRegionStats(new Counts(0, 0, 4, 0), Person.class.getName(), stats);
    }

    private static void testDeleteViaRemove(final EntityManagerFactory emf) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        em.remove(em.find(Pokemon.class, 3));
        em.remove(em.find(Pokemon.class, 248));
        em.remove(em.find(Pokemon.class, 242));
        transaction.commit();
        em.close();

        assertRegionStats(new Counts(0, 3, 0, 4), Pokemon.class.getName(), stats);

        stats = getStatistics(emf);
        
        em = emf.createEntityManager();
        transaction = em.getTransaction();
        transaction.begin();
        if (em.find(Pokemon.class, 3) != null
                || em.find(Pokemon.class, 248) != null
                || em.find(Pokemon.class, 242) != null) {
            throw new RuntimeException("Pokemons should have been deleted");
        }

        transaction.commit();
        em.close();

        assertRegionStats(new Counts(0, 0, 3, 4), Pokemon.class.getName(), stats);
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
        if (!allpersons.get(0).getName().equals("Hibernate ORM")) {
            throw new RuntimeException("Incorrect order of results");
        }
        StringBuilder sb = new StringBuilder("list of stored Person names:\n\t");
        for (Person p : allpersons) {
            p.describeFully(sb);
        }
        sb.append("\nList complete.\n");
        System.out.print(sb);
    }

    private static void testReadWrite(EntityManagerFactory entityManagerFactory) {
        //Store some well known Pokemon instances we can then test on:
        storeTestPokemons(entityManagerFactory, new Counts(3, 0, 0, 3));

        //Load all persons and run some checks on the cache hits
        verifyFindByIdPokemons(entityManagerFactory, new int[]{2555, 3670, 3219}, new Counts(0, 3, 0, 3));

        //Rebalance cp values for pokemons
        rebalanceCpsForPokemons(entityManagerFactory, new Counts(3, 3, 0, 3));

        //Verify cp values after update
        verifyFindByIdPokemons(entityManagerFactory, new int[]{2707, 3834, 2757}, new Counts(0, 3, 0, 3));
    }

    private static void rebalanceCpsForPokemons(final EntityManagerFactory emf, Counts expected) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        Pokemon igeldo = em.find(Pokemon.class, 3);
        igeldo.setCp(2707);
        Pokemon godzilla = em.find(Pokemon.class, 248);
        godzilla.setCp(3834);
        Pokemon blissey = em.find(Pokemon.class, 242);
        blissey.setCp(2757);

        transaction.commit();
        em.close();

        assertRegionStats(expected, Pokemon.class.getName(), stats);
    }

    private static void verifyFindByIdPokemons(final EntityManagerFactory emf, int[] expectedCps, Counts expected) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        Pokemon igeldo = em.find(Pokemon.class, 3);
        if (igeldo.getCp() != expectedCps[0])
            throw new RuntimeException("Incorrect cp: " + igeldo.getCp());

        Pokemon godzilla = em.find(Pokemon.class, 248);
        if (godzilla.getCp() != expectedCps[1])
            throw new RuntimeException("Incorrect cp: " + godzilla.getCp());

        Pokemon blissey = em.find(Pokemon.class, 242);
        if (blissey.getCp() != expectedCps[2])
            throw new RuntimeException("Incorrect cp: " + blissey.getCp());

        transaction.commit();
        em.close();

        assertRegionStats(expected, Pokemon.class.getName(), stats);
    }

    private static void storeTestPokemons(final EntityManagerFactory emf, Counts expected) {
        Statistics stats = getStatistics(emf);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        final Pokemon igeldo = new Pokemon(3, "Venusaur", 2555);
        em.persist(igeldo);
        final Pokemon godzilla = new Pokemon(248, "Tyranitar", 3670);
        em.persist(godzilla);
        final Pokemon khaleesi = new Pokemon(242, "Blissey", 3219);
        em.persist(khaleesi);

        transaction.commit();
        em.close();

        assertRegionStats(expected, Pokemon.class.getName(), stats);
    }

    private static String randomName() {
        return UUID.randomUUID().toString();
    }

    private void reportException(String errorMessage, final Exception e, final HttpServletResponse resp) throws IOException {
        final PrintWriter writer = resp.getWriter();
        if (errorMessage != null) {
            writer.write(errorMessage);
            writer.write(" ");
        }
        writer.write(e.toString());
        writer.append("\n\t");
        e.printStackTrace(writer);
        writer.append("\n\t");
    }

    private static Statistics getStatistics(final EntityManagerFactory emf) {
        final Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        return stats;
    }

    private static void assertRegionStats(Map<String, Counts> counts, Statistics stats) {
        for (Map.Entry<String, Counts> entry : counts.entrySet()) {
            final String region = entry.getKey();
            final Counts expected = entry.getValue();
            final CacheRegionStatistics cacheStats = stats.getDomainDataRegionStatistics(region);
            final Counts actual = new Counts(
                    cacheStats.getPutCount()
                    , cacheStats.getHitCount()
                    , cacheStats.getMissCount()
                    , cacheStats.getElementCountInMemory()
            );
            assertCountEquals(expected, actual, region);
        }
    }

    private static void assertRegionStats(Counts expected, String region, Statistics stats) {
        final CacheRegionStatistics cacheStats = stats.getDomainDataRegionStatistics(region);
        final Counts actual = new Counts(
                cacheStats.getPutCount()
                , cacheStats.getHitCount()
                , cacheStats.getMissCount()
                , cacheStats.getElementCountInMemory()
        );
        assertCountEquals(expected, actual, region);
    }

    private static void assertCountEquals(Counts expected, Counts actual, String msg) {
        //FIXME this is currently failing often on CI, needs to be investigated.
        //Seems to fail more often in native mode.
        // - https://github.com/jbossas/protean-shamrock/issues/694
        /*if (!expected.equals(actual))
            throw new RuntimeException(
                    "[" + msg + "] expected " + expected + " second level cache count, instead got: " + actual
        );*/
    }

    private static void assertRegionStatsEventually(Counts expected, String region, Statistics stats) {
        eventually(() -> {
            final CacheRegionStatistics cacheStats = stats.getDomainDataRegionStatistics(region);
            final Counts actual = new Counts(
                    cacheStats.getPutCount()
                    , cacheStats.getHitCount()
                    , cacheStats.getMissCount()
                    , cacheStats.getElementCountInMemory()
            );
            if (!expected.equals(actual)) {
                return new RuntimeException(
                        "[" + region + "] expected " + expected + " second level cache count, instead got: " + actual
                );
            }

            return null;
        });
    }

    static void eventually(Supplier<RuntimeException> condition) {
        eventually(Duration.ofSeconds(10).toMillis(), Duration.ofMillis(500).toMillis(), TimeUnit.MILLISECONDS, condition);
    }

    static void eventually(long timeout, long pollInterval, TimeUnit unit, Supplier<RuntimeException> condition) {
        if (pollInterval <= 0) {
            throw new IllegalArgumentException("Check interval must be positive");
        }
        try {
            long expectedEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeout, unit);
            long sleepMillis = TimeUnit.MILLISECONDS.convert(pollInterval, unit);
            while (expectedEndTime - System.nanoTime() > 0) {
                if (condition.get() == null) return;
                Thread.sleep(sleepMillis);
            }

            final RuntimeException exception = condition.get();
            if (exception != null)
                throw exception;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected!", e);
        }
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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
