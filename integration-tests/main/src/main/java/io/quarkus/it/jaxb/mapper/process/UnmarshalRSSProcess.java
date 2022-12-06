package io.quarkus.it.jaxb.mapper.process;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import io.quarkus.it.jaxb.mapper.codegen.feed.Feed;
import io.quarkus.it.jaxb.mapper.codegen.feed.Feed.Entry;
import io.quarkus.it.jaxb.object.Category;
import io.quarkus.it.jaxb.object.INews;
import io.quarkus.it.jaxb.object.QuarkusNews;

@ApplicationScoped
public class UnmarshalRSSProcess {

    private static String CODEGEN_PACKAGE = "io.quarkus.it.jaxb.mapper.codegen.feed";
    private static String FEED = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<feed xmlns=\"http://www.w3.org/2005/Atom\" xml:lang=\"fr\">\n" +
            "<link rel=\"self\" type=\"application/atom+xml\" href=\"https://quarkus.io/\" />\n" +
            "\n" +
            "<title>Quarkus</title>\n" +
            "<link href=\"https://quarkus.io/\" />\n" +
            "<updated>2019-08-02T00:00:00+02:00</updated>\n" +
            "\n" +
            "<author><name><![CDATA[Quarkus]]></name></author>\n" +
            "<id>https://quarkus.io/blog/</id>\n" +
            "<entry>\n" +
            "<author><name><![CDATA[Emmanuel Bernard]]></name></author>\n" +
            "<updated>2019-08-01T00:00:00+02:00</updated>\n" +
            "<published>2019-08-01T00:00:00+02:00</published>\n" +
            "<id>https://quarkus.io/blog/hibernate-orm-config-profiles/</id>\n" +
            "<link href=\"https://quarkus.io/blog/hibernate-orm-config-profiles/\"/>\n" +
            "<title type=\"html\"><![CDATA[Tips to use Hibernate ORM with Quarkus profiles and live coding mode]]></title>\n" +
            "\n" +
            "<category term=\"tips\" scheme=\"https://quarkus.io/blog/category/tips\" label=\"tips\"/>\n" +
            "<content type=\"html\" xml:base=\"https://quarkus.io/blog/hibernate-orm-config-profiles/\"><![CDATA[\n" +
            "Hibernate ORM lets you generate or update the database schema. Let's explore when to use such option in combination with live coding."
            +
            "]]></content>\n" +
            "</entry>\n" +
            "</feed>";

    public Collection<INews> retrieveLastNews(int nbNews, Category category) {
        return getQuarkusNews(getFeed()).subList(0, nbNews);
    }

    private Feed getFeed() {
        try {
            JAXBContext jc = JAXBContext.newInstance(CODEGEN_PACKAGE);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            return (Feed) unmarshaller.unmarshal(new StringReader(FEED));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<INews> getQuarkusNews(Feed feed) {
        List<INews> quarkusNews = new ArrayList<>();
        for (Entry entry : feed.getEntry()) {
            quarkusNews.add(mapToQuarkusNews(entry));
        }
        return quarkusNews;
    }

    private QuarkusNews mapToQuarkusNews(Entry entry) {
        QuarkusNews quarkusNews = new QuarkusNews();
        quarkusNews.setTitle(entry.getTitle());
        quarkusNews.setDescription(entry.getContent());
        quarkusNews.setContent(entry.getContent());
        quarkusNews.setAuthor(entry.getAuthor().getName());
        quarkusNews.setUrl(entry.getLink().getHref());
        return quarkusNews;
    }
}
