package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.graph.GraphCommands;
import io.quarkus.redis.datasource.graph.GraphQueryResponseItem;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.vertx.core.json.JsonObject;

@RequiresCommand("graph.query")
public class GraphCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    private GraphCommands<String> graph;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        graph = ds.graph();
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(graph.getDataSource());
    }

    @Test
    public void testQuery() {
        assertThat(graph.graphList()).isEmpty();

        String q = "CREATE (:Rider {name:'Valentino Rossi'})-[:rides]->(:Team {name:'Yamaha'}), (:Rider {name:'Dani Pedrosa'})-[:rides]->(:Team {name:'Honda'}), (:Rider {name:'Andrea Dovizioso'})-[:rides]->(:Team {name:'Ducati'})";
        assertThat(graph.graphQuery("moto", q)).isEmpty();

        q = "MATCH (r:Rider)-[:rides]->(t:Team) WHERE t.name = 'Yamaha' RETURN r.name, t.name";
        List<Map<String, GraphQueryResponseItem>> list = graph.graphQuery("moto", q);

        assertThat(list).hasSize(1).allSatisfy(map -> {
            GraphQueryResponseItem.ScalarItem driver = map.get("r.name").asScalarItem();
            GraphQueryResponseItem.ScalarItem team = map.get("t.name").asScalarItem();
            assertThat(driver.asString()).isEqualTo("Valentino Rossi");
            assertThat(driver.name()).isEqualTo("r.name");
            assertThat(team.asString()).isEqualTo("Yamaha");
            assertThat(team.name()).isEqualTo("t.name");
        });

        q = "MATCH (r:Rider)-[:rides]->(t:Team) WHERE t.name = 'Missing' RETURN r.name, t.name";
        list = graph.graphQuery("moto", q);
        assertThat(list).hasSize(0);

        q = "MATCH (r:Rider)-[:rides]->(t:Team) WHERE t.name = 'Yamaha' RETURN r.name, t.missing";
        list = graph.graphQuery("moto", q);
        assertThat(list).hasSize(1).allSatisfy(map -> {
            GraphQueryResponseItem.ScalarItem driver = map.get("r.name").asScalarItem();
            GraphQueryResponseItem.ScalarItem missing = map.get("t.missing").asScalarItem();
            assertThat(driver.asString()).isEqualTo("Valentino Rossi");
            assertThat(driver.name()).isEqualTo("r.name");
            assertThat(missing.isNull()).isTrue();
            assertThat(missing.name()).isEqualTo("t.missing");
        });

        q = "MATCH (r:Rider)-[:rides]->(t:Team {name:'Ducati'}) RETURN count(r)";
        list = graph.graphQuery("moto", q);
        assertThat(list).hasSize(1).allSatisfy(map -> {
            GraphQueryResponseItem.ScalarItem count = map.get("count(r)").asScalarItem();
            assertThat(count.asInteger()).isEqualTo(1);
            assertThat(count.name()).isEqualTo("count(r)");
        });

        assertThat(graph.graphList()).containsExactly("moto");
        assertThat(graph.graphExplain("moto", q)).isNotBlank();
    }

    @Test
    public void testComplexGraph() {
        String q = "CREATE (aldis:actor {name: \"Aldis Hodge\", birth_year: 1986}),\n"
                + "                         (oshea:actor {name: \"OShea Jackson\", birth_year: 1991}),\n"
                + "                         (corey:actor {name: \"Corey Hawkins\", birth_year: 1988}),\n"
                + "                         (neil:actor {name: \"Neil Brown\", birth_year: 1980}),\n"
                + "                         (compton:movie {title: \"Straight Outta Compton\", genre: \"Biography\", votes: 127258, rating: 7.9, year: 2015}),\n"
                + "                         (neveregoback:movie {title: \"Never Go Back\", genre: \"Action\", votes: 15821, rating: 6.4, year: 2016}),\n"
                + "                         (aldis)-[:act]->(neveregoback),\n"
                + "                         (aldis)-[:act]->(compton),\n"
                + "                         (oshea)-[:act]->(compton),\n"
                + "                         (corey)-[:act]->(compton),\n"
                + "                         (neil)-[:act]->(compton)";

        assertThat(graph.graphQuery("imdb", q)).isEmpty();

        q = "MATCH (a:actor)-[:act]->(m:movie {title:\"Straight Outta Compton\"})\n"
                + "RETURN m.title, SUM(2020-a.birth_year), MAX(2020-a.birth_year), MIN(2020-a.birth_year), AVG(2020-a.birth_year)";

        List<Map<String, GraphQueryResponseItem>> imdb = graph.graphQuery("imdb", q);
        assertThat(imdb).hasSize(1);
        Map<String, GraphQueryResponseItem> map = imdb.get(0);
        assertThat(map.get("m.title").asScalarItem().asString()).isEqualTo("Straight Outta Compton");
        assertThat(map.get("SUM(2020-a.birth_year)").asScalarItem().asInteger()).isEqualTo(135);
        assertThat(map.get("MAX(2020-a.birth_year)").asScalarItem().asInteger()).isEqualTo(40);
        assertThat(map.get("MIN(2020-a.birth_year)").asScalarItem().asInteger()).isEqualTo(29);
        assertThat(map.get("AVG(2020-a.birth_year)").asScalarItem().asDouble()).isEqualTo(33.75);

        q = "MATCH (actor)-[:act]->(movie) RETURN actor.name, COUNT(movie.title) AS movies_count ORDER BY\n"
                + "movies_count DESC";
        imdb = graph.graphQuery("imdb", q);
        assertThat(imdb).hasSize(4);
    }

    @Test
    void testNodeRelationAndScalarParsing() {
        String q = "CREATE (:person {name:'Pam', age:27})-[:works {since: 2010}]->(:employer {name:'Dunder Mifflin'})";
        graph.graphQuery(key, q);
        q = "MATCH (n1)-[r]->(n2) RETURN n1, r, n2.name";
        List<Map<String, GraphQueryResponseItem>> list = graph.graphQuery(key, q);
        assertThat(list).hasSize(1);
        Map<String, GraphQueryResponseItem> map = list.get(0);
        assertThat(map).hasSize(3);
        assertThat(map.get("n1")).isInstanceOf(GraphQueryResponseItem.NodeItem.class);
        assertThat(map.get("n1").asNodeItem().labels()).containsExactly("person");
        assertThat(map.get("n1").asNodeItem().id()).isEqualTo(0);
        assertThat(map.get("n1").asNodeItem().get("name").asString()).isEqualTo("Pam");
        assertThat(map.get("n1").asNodeItem().get("age").asInteger()).isEqualTo(27);

        assertThat(map.get("r")).isInstanceOf(GraphQueryResponseItem.RelationItem.class);
        assertThat(map.get("r").asRelationItem().source()).isEqualTo(0);
        assertThat(map.get("r").asRelationItem().destination()).isEqualTo(1);
        assertThat(map.get("r").asRelationItem().id()).isEqualTo(0);
        assertThat(map.get("r").asRelationItem().type()).isEqualTo("works");
        assertThat(map.get("r").asRelationItem().get("since").asInteger()).isEqualTo(2010);

        assertThat(map.get("n2.name")).isInstanceOf(GraphQueryResponseItem.ScalarItem.class);
        assertThat(map.get("n2.name").asScalarItem().asString()).isEqualTo("Dunder Mifflin");
    }

    @Test
    void iterativeGraphCreation() {
        graph.graphQuery("cities", "CREATE (:City {name:'Paris', longitude:2.349014, latitude:48.864716})");
        graph.graphQuery("cities", "CREATE (:City {name:'Lyon', longitude:4.834277, latitude:45.763420})");
        graph.graphQuery("cities", "CREATE (:City {name:'Valence', longitude:4.8924, latitude:45.763420})");
        graph.graphQuery("cities", "CREATE (:City {name:'Marseille', longitude:5.3698, latitude:43.2965})");
        graph.graphQuery("cities", "CREATE (:City {name:'Bordeaux'})");
        graph.graphQuery("cities", "CREATE (:City {name:'Nancy'})");

        graph.graphQuery("cities", "CREATE (:Station {name:'Gare de Lyon'})");
        graph.graphQuery("cities", "CREATE (:Station {name:'Gare Montparnasse'})");
        graph.graphQuery("cities", "CREATE (:Station {name:'Gare de l\\'Est'})");
        graph.graphQuery("cities", "CREATE (:Station {name:'Gare du Nord'})");

        graph.graphQuery("cities",
                "MATCH (c:City), (s:Station) WHERE c.name ='Paris' and s.name='Gare de Lyon' CREATE (c)-[:is_in]->(s)");
        graph.graphQuery("cities",
                "MATCH (c:City), (s:Station) WHERE c.name ='Paris' and s.name='Gare Montparnasse' CREATE (c)-[:is_in]->(s)");
        graph.graphQuery("cities",
                "MATCH (c:City), (s:Station) WHERE c.name ='Paris' and s.name='Gare de l\\'Est' CREATE (c)-[:is_in]->(s)");
        graph.graphQuery("cities",
                "MATCH (c:City), (s:Station) WHERE c.name ='Paris' and s.name='Gare du Nord' CREATE (c)-[:is_in]->(s)");

        graph.graphQuery("cities",
                "MATCH (s:Station), (c:City) WHERE c.name ='Valence' and s.name='Gare de Lyon' CREATE (s)-[:connect]->(c)");
        graph.graphQuery("cities",
                "MATCH (s:Station), (c:City) WHERE c.name ='Marseille' and s.name='Gare de Lyon' CREATE (s)-[:connect]->(c)");
        graph.graphQuery("cities",
                "MATCH (s:Station), (c:City) WHERE c.name ='Lyon' and s.name='Gare de Lyon' CREATE (s)-[:connect]->(c)");
        graph.graphQuery("cities",
                "MATCH (s:Station), (c:City) WHERE c.name ='Bordeaux' and s.name='Gare Montparnasse' CREATE (s)-[:connect]->(c)");
        graph.graphQuery("cities",
                "MATCH (s:Station), (c:City) WHERE c.name ='Nancy' and s.name='Gare de l\\'Est' CREATE (s)-[:connect]->(c)");

        var res = graph.graphQuery("cities",
                "MATCH (c1:City {name:'Paris'})-[:is_in]->(s:Station)-[:connect]->(c:City) WHERE s.name='Gare de Lyon' RETURN c.name, c");
        assertThat(res).hasSize(3);
        res = graph.graphQuery("cities",
                "MATCH (c1:City {name:'Paris'})-[:is_in]->(s:Station)-[:connect]->(c:City) WHERE s.name='Gare de Lyon' RETURN c");
        assertThat(res).hasSize(3)
                .anySatisfy(map -> assertThat(map.get("c").asNodeItem().get("name").asString()).isEqualTo("Lyon"))
                .anySatisfy(map -> assertThat(map.get("c").asNodeItem().get("name").asString()).isEqualTo("Marseille"))
                .anySatisfy(map -> assertThat(map.get("c").asNodeItem().get("name").asString()).isEqualTo("Valence"));

        res = graph.graphQuery("cities",
                "MATCH (c1:City {name:'Paris'})-[:is_in]->(s:Station)-[:connect]->(c:City) WHERE s.name='Gare de Lyon' RETURN toJson(c) as val");
        assertThat(res).hasSize(3);
        assertThat(res).allSatisfy(
                map -> assertThat(new JsonObject(map.get("val").asScalarItem().asString()).getJsonArray("labels"))
                        .containsExactly("City"));
    }

}
