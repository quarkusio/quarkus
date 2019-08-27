package io.quarkus.it.infinispan.client;

import java.io.IOException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.infinispan.protostream.MessageMarshaller;

public class MagazineMarshaller implements MessageMarshaller<Magazine> {
    @Override
    public Magazine readFrom(ProtoStreamReader reader) throws IOException {
        String name = reader.readString("name");
        YearMonth yearMonth = YearMonth.of(reader.readInt("publicationYear"), reader.readInt("publicationMonth"));
        List<String> stories = reader.readCollection("stories", new ArrayList<>(), String.class);
        return new Magazine(name, yearMonth, stories);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Magazine magazine) throws IOException {
        writer.writeString("name", magazine.getName());
        YearMonth yearMonth = magazine.getPublicationYearMonth();
        writer.writeInt("publicationYear", yearMonth.getYear());
        writer.writeInt("publicationMonth", yearMonth.getMonthValue());
        writer.writeCollection("stories", magazine.getStories(), String.class);
    }

    @Override
    public Class<? extends Magazine> getJavaClass() {
        return Magazine.class;
    }

    @Override
    public String getTypeName() {
        return "magazine_sample.Magazine";
    }
}
