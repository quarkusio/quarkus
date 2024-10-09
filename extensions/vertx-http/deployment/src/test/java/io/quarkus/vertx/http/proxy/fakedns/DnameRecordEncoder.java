package io.quarkus.vertx.http.proxy.fakedns;

import org.apache.directory.server.dns.io.encoder.ResourceRecordEncoder;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.mina.core.buffer.IoBuffer;

public class DnameRecordEncoder extends ResourceRecordEncoder {

    protected void putResourceRecordData(IoBuffer byteBuffer, ResourceRecord record) {
        String domainName = record.get(DnsAttribute.DOMAIN_NAME);
        putDomainName(byteBuffer, domainName);
    }
}
