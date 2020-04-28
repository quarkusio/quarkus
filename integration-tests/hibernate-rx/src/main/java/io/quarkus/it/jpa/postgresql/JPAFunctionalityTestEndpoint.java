package io.quarkus.it.jpa.postgresql;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.rx.RxSession;
import org.hibernate.rx.RxSessionFactory;
import org.hibernate.rx.mutiny.Mutiny;
import org.hibernate.rx.service.RxConnection;
import org.hibernate.rx.service.initiator.RxConnectionPoolProvider;
import org.hibernate.rx.util.impl.RxUtil;

import io.smallrye.mutiny.Uni;
import io.vertx.axle.sqlclient.Tuple;

@Path("/tests")
public class JPAFunctionalityTestEndpoint {

    private RxSession session;
    private SessionFactory sessionFactory;
    private RxConnectionPoolProvider poolProvider;

    //    @PersistenceUnit(unitName = "templatePU")
    //    EntityManagerFactory entityManagerFactory;

    private Uni<String> selectNameFromId(Integer id) {
        CompletionStage<String> result = connection().preparedQuery(
                "SELECT name FROM Pig WHERE id = $1", Tuple.of(id)).thenApply(
                        rowSet -> {
                            if (rowSet.size() == 1) {
                                // Only one result
                                return rowSet.iterator().next().getString(0);
                            } else if (rowSet.size() > 1) {
                                throw new AssertionError("More than one result returned: " + rowSet.size());
                            } else {
                                // Size 0
                                return null;
                            }
                        });
        return Uni.createFrom().completionStage(result);
    }

    @GET
    @Path("/reactiveFind1")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<GuineaPig> reactiveFind1() {
        System.out.println("@AGG in reactiveFind1");
        final GuineaPig expectedPig = new GuineaPig(5, "Aloi");
        return populateDB()
                .onItem().produceUni(i -> openMutinySession())
                .onItem().produceUni(session -> session.find(GuineaPig.class, expectedPig.getId()));
        //				.onItem().invoke( actualPig -> assertThatPigsAreEqual( context, expectedPig, actualPig ) )
        //				.convert().toCompletionStage();
    }

    //	@Test
    //	public void reactiveFind2(TestContext context) {
    //		final GuineaPig expectedPig = new GuineaPig( 5, "Aloi" );
    //		test(
    //				context,
    //				populateDB()
    //						.flatMap( i -> openMutinySession() )
    //						.flatMap( session -> session.find( GuineaPig.class, expectedPig.getId() ) )
    //						.onItem().invoke( actualPig -> assertThatPigsAreEqual( context, expectedPig, actualPig ) )
    //						.convert().toCompletionStage()
    //		);
    //	}
    //
    //	@Test
    //	public void reactivePersist1(TestContext context) {
    //		test(
    //				context,
    //				openMutinySession()
    //						.onItem().produceUni( s -> s.persist( new GuineaPig( 10, "Tulip" ) ) )
    //						.onItem().produceUni( s -> s.flush() )
    //						.onItem().produceUni( v -> selectNameFromId( 10 ) )
    //						.onItem().invoke( selectRes -> context.assertEquals( "Tulip", selectRes ) )
    //						.convert().toCompletionStage()
    //		);
    //	}
    //
    //	@Test
    //	public void reactivePersist2(TestContext context) {
    //		test(
    //				context,
    //				openMutinySession()
    //						.flatMap( s -> s.persist( new GuineaPig( 10, "Tulip" ) ) )
    //						.flatMap( s -> s.flush() )
    //						.flatMap( v -> selectNameFromId( 10 ) )
    //						.map( selectRes -> context.assertEquals( "Tulip", selectRes ) )
    //						.convert().toCompletionStage()
    //		);
    //	}
    //
    //	@Test
    //	public void reactiveRemoveTransientEntity1(TestContext context) {
    //		test(
    //				context,
    //				populateDB()
    //						.onItem().produceUni( v -> selectNameFromId( 5 ) )
    //						.onItem().invoke( name -> context.assertNotNull( name ) )
    //						.onItem().produceUni( v -> openMutinySession() )
    //						.onItem().produceUni( session -> session.remove( new GuineaPig( 5, "Aloi" ) ) )
    //						.onItem().produceUni( session -> session.flush() )
    //						.onItem().produceUni( v -> selectNameFromId( 5 ) )
    //						.onItem().invoke( ret -> context.assertNull( ret ) )
    //						.convert().toCompletionStage()
    //		);
    //	}
    //
    //	@Test
    //	public void reactiveRemoveTransientEntity2(TestContext context) {
    //		test(
    //				context,
    //				populateDB()
    //						.flatMap( v -> selectNameFromId( 5 ) )
    //						.map( name -> context.assertNotNull( name ) )
    //						.flatMap( v -> openMutinySession() )
    //						.flatMap( session -> session.remove( new GuineaPig( 5, "Aloi" ) ) )
    //						.flatMap( session -> session.flush() )
    //						.flatMap( v -> selectNameFromId( 5 ) )
    //						.map( ret -> context.assertNull( ret ) )
    //						.convert().toCompletionStage()
    //		);
    //	}
    //
    //	@Test
    //	public void reactiveRemoveManagedEntity1(TestContext context) {
    //		test(
    //				context,
    //				populateDB()
    //						.onItem().produceUni( v -> openMutinySession() )
    //						.onItem().produceUni( session ->
    //								session.find( GuineaPig.class, 5 )
    //										.onItem().produceUni( aloi -> session.remove( aloi.get() ) )
    //										.onItem().produceUni( v -> session.flush() )
    //										.onItem().produceUni( v -> selectNameFromId( 5 ) )
    //										.onItem().invoke( ret -> context.assertNull( ret ) ) )
    //						.convert().toCompletionStage()
    //		);
    //	}
    //
    //	@Test
    //	public void reactiveRemoveManagedEntity2(TestContext context) {
    //		test(
    //				context,
    //				populateDB()
    //						.flatMap( v -> openMutinySession() )
    //						.flatMap( session ->
    //							session.find( GuineaPig.class, 5 )
    //								.flatMap( aloi -> session.remove( aloi.get() ) )
    //								.flatMap( v -> session.flush() )
    //								.flatMap( v -> selectNameFromId( 5 ) )
    //								.map( ret -> context.assertNull( ret ) ) )
    //								.convert().toCompletionStage()
    //		);
    //	}
    //
    //	@Test
    //	public void reactiveUpdate(TestContext context) {
    //		final String NEW_NAME = "Tina";
    //		test(
    //				context,
    //				populateDB()
    //						.flatMap( v -> openMutinySession() )
    //						.flatMap( session ->
    //							session.find( GuineaPig.class, 5 )
    //								.onItem().invoke( o -> {
    //									GuineaPig pig = o.orElseThrow( () -> new AssertionError( "Guinea pig not found" ) );
    //									// Checking we are actually changing the name
    //									context.assertNotEquals( pig.getName(), NEW_NAME );
    //									pig.setName( NEW_NAME );
    //								} )
    //								.flatMap( v -> session.flush() )
    //								.flatMap( v -> selectNameFromId( 5 ) )
    //								.map( name -> context.assertEquals( NEW_NAME, name ) ) )
    //						.convert().toCompletionStage()
    //		);
    //	}

    //	private void assertThatPigsAreEqual(TestContext context, GuineaPig expected, Optional<GuineaPig> actual) {
    //		context.assertTrue( actual.isPresent() );
    //		context.assertEquals( expected.getId(), actual.get().getId() );
    //		context.assertEquals( expected.getName(), actual.get().getName() );
    //	}

    public void before() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(constructConfiguration().getProperties())
                .build();

        sessionFactory = constructConfiguration().buildSessionFactory(registry);
        poolProvider = registry.getService(RxConnectionPoolProvider.class);

        // EITHER WAY WORKS:
        // session = sessionFactory.openSession().unwrap(RxSession.class);
        session = sessionFactory.unwrap(RxSessionFactory.class).openRxSession();
    }

    public void after() {
        if (session != null) {
            session.close();
        }
        sessionFactory.close();
    }

    protected CompletionStage<RxSession> openSession() {
        return RxUtil.nullFuture().thenApply(v -> {
            if (session != null) {
                session.close();
            }
            session = sessionFactory.unwrap(RxSessionFactory.class).openRxSession();
            return session;
        });
    }

    protected RxConnection connection() {
        return poolProvider.getConnection();
    }

    private Uni<Integer> populateDB() {
        return Uni.createFrom().completionStage(connection().update("INSERT INTO Pig (id, name) VALUES (5, 'Aloi')"));
    }

    private CompletionStage<Integer> cleanDB() {
        return connection().update("DELETE FROM Pig");
    }

    protected Configuration constructConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, "create-drop");
        String pgUrl = System.getProperty("quarkus.datasource.jdbc.url");
        System.out.println("@AGG PG URL IS: " + pgUrl);
        configuration.setProperty(AvailableSettings.URL, pgUrl);
        configuration.setProperty(AvailableSettings.SHOW_SQL, "true");
        return configuration;
    }

    protected Uni<Mutiny.Session> openMutinySession() {
        return Uni.createFrom().completionStage(openSession()).map(Mutiny.Session::new);
    }

}
