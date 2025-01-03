/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.quarkus.vertx.http.proxy.fakedns;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.directory.server.dns.DnsServer;
import org.apache.directory.server.dns.io.encoder.ResourceRecordEncoder;
import org.apache.directory.server.dns.messages.*;
import org.apache.directory.server.dns.protocol.DnsProtocolHandler;
import org.apache.directory.server.dns.protocol.DnsTcpDecoder;
import org.apache.directory.server.dns.protocol.DnsUdpDecoder;
import org.apache.directory.server.dns.protocol.DnsUdpEncoder;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.transport.socket.DatagramSessionConfig;

/**
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
public final class FakeDNSServer extends DnsServer {

    public static RecordStore A_store(Map<String, String> entries) {
        return questionRecord -> entries.entrySet().stream().map(entry -> {
            return a(entry.getKey(), 100).ipAddress(entry.getValue());
        }).collect(Collectors.toSet());
    }

    public static RecordStore A_store(Function<String, String> entries) {
        return questionRecord -> {
            String res = entries.apply(questionRecord.getDomainName());
            if (res != null) {
                return Collections.singleton(a(questionRecord.getDomainName(), 100).ipAddress(res));
            }
            return Collections.emptySet();
        };
    }

    public static final int PORT = 53530;
    public static final String IP_ADDRESS = "127.0.0.1";

    private String ipAddress = IP_ADDRESS;
    private int port = PORT;
    private volatile RecordStore store;
    private List<IoAcceptor> acceptors;
    private final Deque<DnsMessage> currentMessage = new ArrayDeque<>();

    public FakeDNSServer() {
    }

    public RecordStore store() {
        return store;
    }

    public FakeDNSServer store(RecordStore store) {
        this.store = store;
        return this;
    }

    public synchronized DnsMessage pollMessage() {
        return currentMessage.poll();
    }

    public InetSocketAddress localAddress() {
        return (InetSocketAddress) getTransports()[0].getAcceptor().getLocalAddress();
    }

    public FakeDNSServer ipAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public FakeDNSServer port(int p) {
        port = p;
        return this;
    }

    public FakeDNSServer testResolveA(final String ipAddress) {
        return testResolveA(Collections.singletonMap("vertx.io", ipAddress));
    }

    public FakeDNSServer testResolveA(Map<String, String> entries) {
        return store(A_store(entries));
    }

    public FakeDNSServer testResolveA(Function<String, String> entries) {
        return store(A_store(entries));
    }

    public FakeDNSServer testResolveAAAA(final String ipAddress) {
        return store(questionRecord -> Collections.singleton(aaaa("vertx.io", 100).ipAddress(ipAddress)));
    }

    public FakeDNSServer testResolveMX(final int prio, final String mxRecord) {
        return store(questionRecord -> Collections.singleton(mx("vertx.io", 100)
                .set(DnsAttribute.MX_PREFERENCE, String.valueOf(prio))
                .set(DnsAttribute.DOMAIN_NAME, mxRecord)));
    }

    public FakeDNSServer testResolveTXT(final String txt) {
        return store(questionRecord -> Collections.singleton(txt("vertx.io", 100)
                .set(DnsAttribute.CHARACTER_STRING, txt)));
    }

    public FakeDNSServer testResolveNS(final String ns) {
        return store(questionRecord -> Collections.singleton(ns("vertx.io", 100)
                .set(DnsAttribute.DOMAIN_NAME, ns)));
    }

    public FakeDNSServer testResolveCNAME(final String cname) {
        return store(questionRecord -> Collections.singleton(cname("vertx.io", 100)
                .set(DnsAttribute.DOMAIN_NAME, cname)));
    }

    public FakeDNSServer testResolvePTR(final String ptr) {
        return store(questionRecord -> Collections.singleton(ptr("vertx.io", 100)
                .set(DnsAttribute.DOMAIN_NAME, ptr)));
    }

    public FakeDNSServer testResolveSRV(String name, int priority, int weight, int port, String target) {
        return store(questionRecord -> Collections.singleton(srv(name, 100)
                .set(DnsAttribute.SERVICE_PRIORITY, priority)
                .set(DnsAttribute.SERVICE_WEIGHT, weight)
                .set(DnsAttribute.SERVICE_PORT, port)
                .set(DnsAttribute.DOMAIN_NAME, target)));
    }

    public FakeDNSServer testResolveDNAME(final String dname) {
        return store(questionRecord -> Collections.singleton(dname("vertx.io", 100)
                .set(DnsAttribute.DOMAIN_NAME, dname)));
    }

    public FakeDNSServer testResolveSRV2(final int priority, final int weight, final int basePort, final String target) {
        return store(questionRecord -> IntStream
                .range(0, 2)
                .mapToObj(i -> srv(target, 100)
                        .set(DnsAttribute.SERVICE_PRIORITY, priority)
                        .set(DnsAttribute.SERVICE_WEIGHT, weight)
                        .set(DnsAttribute.SERVICE_PORT, basePort + i)
                        .set(DnsAttribute.DOMAIN_NAME, "svc" + i + ".vertx.io."))
                .collect(Collectors.toSet()));
    }

    public static Record a(String domainName, int ttl) {
        return new Record(domainName, RecordType.A, RecordClass.IN, ttl);
    }

    public static Record aaaa(String domainName, int ttl) {
        return new Record(domainName, RecordType.AAAA, RecordClass.IN, ttl);
    }

    public static Record mx(String domainName, int ttl) {
        return new Record(domainName, RecordType.MX, RecordClass.IN, ttl);
    }

    public static Record txt(String domainName, int ttl) {
        return new Record(domainName, RecordType.TXT, RecordClass.IN, ttl);
    }

    public static Record ns(String domainName, int ttl) {
        return new Record(domainName, RecordType.NS, RecordClass.IN, ttl);
    }

    public static Record cname(String domainName, int ttl) {
        return new Record(domainName, RecordType.CNAME, RecordClass.IN, ttl);
    }

    public static Record ptr(String domainName, int ttl) {
        return new Record(domainName, RecordType.PTR, RecordClass.IN, ttl);
    }

    public static Record srv(String domainName, int ttl) {
        return new Record(domainName, RecordType.SRV, RecordClass.IN, ttl);
    }

    public static Record dname(String domainName, int ttl) {
        return new Record(domainName, RecordType.DNAME, RecordClass.IN, ttl);
    }

    public static Record record(String domainName, RecordType recordType, RecordClass recordClass, int ttl) {
        return new Record(domainName, recordType, recordClass, ttl);
    }

    public static class Record extends HashMap<String, String> implements ResourceRecord {

        private final String domainName;
        private final RecordType recordType;
        private final RecordClass recordClass;
        private final int ttl;

        public Record(String domainName, RecordType recordType, RecordClass recordClass, int ttl) {
            this.domainName = domainName;
            this.recordType = recordType;
            this.recordClass = recordClass;
            this.ttl = ttl;
        }

        public Record ipAddress(String ipAddress) {
            return set(DnsAttribute.IP_ADDRESS, ipAddress);
        }

        public Record set(String name, Object value) {
            put(name, "" + value);
            return this;
        }

        @Override
        public String getDomainName() {
            return domainName;
        }

        @Override
        public RecordType getRecordType() {
            return recordType;
        }

        @Override
        public RecordClass getRecordClass() {
            return recordClass;
        }

        @Override
        public int getTimeToLive() {
            return ttl;
        }

        @Override
        public String get(String id) {
            return get((Object) id);
        }
    }

    public FakeDNSServer testLookup4(String ip) {
        return store(questionRecord -> {
            Set<ResourceRecord> set = new HashSet<>();
            if (questionRecord.getRecordType() == RecordType.A) {
                set.add(a("vertx.io", 100).ipAddress(ip));
            }
            return set;
        });
    }

    public FakeDNSServer testLookup6(String ip) {
        return store(questionRecord -> {
            Set<ResourceRecord> set = new HashSet<>();
            if (questionRecord.getRecordType() == RecordType.AAAA) {
                set.add(aaaa("vertx.io", 100).ipAddress(ip));
            }
            return set;
        });
    }

    public FakeDNSServer testLookupNonExisting() {
        return store(questionRecord -> null);
    }

    public FakeDNSServer testReverseLookup(final String ptr) {
        return store(questionRecord -> Collections.singleton(ptr(ptr, 100)
                .set(DnsAttribute.DOMAIN_NAME, "vertx.io")));
    }

    public FakeDNSServer testResolveASameServer(final String ipAddress) {
        return store(A_store(Collections.singletonMap("vertx.io", ipAddress)));
    }

    public FakeDNSServer testLookup4CNAME(final String cname, final String ip) {
        return store(questionRecord -> {
            // use LinkedHashSet since the order of the result records has to be preserved to make sure the unit test fails
            Set<ResourceRecord> set = new LinkedHashSet<>();
            ResourceRecordModifier rm = new ResourceRecordModifier();
            set.add(cname("vertx.io", 100).set(DnsAttribute.DOMAIN_NAME, cname));
            set.add(a(cname, 100).ipAddress(ip));
            return set;
        });
    }

    @Override
    public void start() throws IOException {

        DnsProtocolHandler handler = new DnsProtocolHandler(this, question -> {
            RecordStore actual = store;
            if (actual == null) {
                return Collections.emptySet();
            } else {
                return actual.getRecords(question);
            }
        }) {
            @Override
            public void sessionCreated(IoSession session) {
                // Use our own codec to support AAAA testing
                if (session.getTransportMetadata().isConnectionless()) {
                    session.getFilterChain().addFirst("codec", new ProtocolCodecFilter(new TestDnsProtocolUdpCodecFactory()));
                } else {
                    session.getFilterChain().addFirst("codec", new ProtocolCodecFilter(new TestDnsProtocolTcpCodecFactory()));
                }
            }

            @Override
            public void messageReceived(IoSession session, Object message) {
                if (message instanceof DnsMessage) {
                    synchronized (FakeDNSServer.this) {
                        currentMessage.add((DnsMessage) message);
                    }
                }
                super.messageReceived(session, message);
            }
        };

        UdpTransport udpTransport = new UdpTransport(ipAddress, port);
        ((DatagramSessionConfig) udpTransport.getAcceptor().getSessionConfig()).setReuseAddress(true);
        TcpTransport tcpTransport = new TcpTransport(ipAddress, port);
        tcpTransport.getAcceptor().getSessionConfig().setReuseAddress(true);

        setTransports(udpTransport, tcpTransport);

        for (Transport transport : getTransports()) {
            IoAcceptor acceptor = transport.getAcceptor();

            acceptor.setHandler(handler);

            // Start the listener
            acceptor.bind();
        }
    }

    @Override
    public void stop() {
        for (Transport transport : getTransports()) {
            transport.getAcceptor().dispose();
        }
    }

    public static class VertxResourceRecord implements ResourceRecord {

        private final String ipAddress;
        private final String domainName;
        private boolean isTruncated;

        public VertxResourceRecord(String domainName, String ipAddress) {
            this.domainName = domainName;
            this.ipAddress = ipAddress;
        }

        public boolean isTruncated() {
            return isTruncated;
        }

        public VertxResourceRecord setTruncated(boolean truncated) {
            isTruncated = truncated;
            return this;
        }

        @Override
        public String getDomainName() {
            return domainName;
        }

        @Override
        public RecordType getRecordType() {
            return RecordType.A;
        }

        @Override
        public RecordClass getRecordClass() {
            return RecordClass.IN;
        }

        @Override
        public int getTimeToLive() {
            return 100;
        }

        @Override
        public String get(String id) {
            return DnsAttribute.IP_ADDRESS.equals(id) ? ipAddress : null;
        }
    }

    private static final ResourceRecordEncoder TestAAAARecordEncoder = new ResourceRecordEncoder() {
        @Override
        protected void putResourceRecordData(IoBuffer ioBuffer, ResourceRecord resourceRecord) {
            if (!resourceRecord.get(DnsAttribute.IP_ADDRESS).equals("::1")) {
                throw new IllegalStateException("Only supposed to be used with IPV6 address of ::1");
            }
            // encode the ::1
            ioBuffer.put(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 });
        }
    };

    private final DnsMessageEncoder encoder = new DnsMessageEncoder();

    private void encode(DnsMessage dnsMessage, IoBuffer buf) {

        // Hack
        if (dnsMessage.getAnswerRecords().size() == 1 && dnsMessage.getAnswerRecords().get(0) instanceof VertxResourceRecord) {
            VertxResourceRecord vrr = (VertxResourceRecord) dnsMessage.getAnswerRecords().get(0);

            DnsMessageModifier modifier = new DnsMessageModifier();
            modifier.setTransactionId(dnsMessage.getTransactionId());
            modifier.setMessageType(dnsMessage.getMessageType());
            modifier.setOpCode(dnsMessage.getOpCode());
            modifier.setAuthoritativeAnswer(dnsMessage.isAuthoritativeAnswer());
            modifier.setTruncated(dnsMessage.isTruncated());
            modifier.setRecursionDesired(dnsMessage.isRecursionDesired());
            modifier.setRecursionAvailable(dnsMessage.isRecursionAvailable());
            modifier.setReserved(dnsMessage.isReserved());
            modifier.setAcceptNonAuthenticatedData(dnsMessage.isAcceptNonAuthenticatedData());
            modifier.setResponseCode(dnsMessage.getResponseCode());
            modifier.setQuestionRecords(dnsMessage.getQuestionRecords());
            modifier.setAnswerRecords(dnsMessage.getAnswerRecords());
            modifier.setAuthorityRecords(dnsMessage.getAuthorityRecords());
            modifier.setAdditionalRecords(dnsMessage.getAdditionalRecords());

            modifier.setTruncated(vrr.isTruncated);

            dnsMessage = modifier.getDnsMessage();
        }

        encoder.encode(buf, dnsMessage);

        for (ResourceRecord record : dnsMessage.getAnswerRecords()) {
            // This is a hack to allow to also test for AAAA resolution as DnsMessageEncoder does not support it and it
            // is hard to extend, because the interesting methods are private...
            // In case of RecordType.AAAA we need to encode the RecordType by ourself
            if (record.getRecordType() == RecordType.AAAA) {
                try {
                    TestAAAARecordEncoder.put(buf, record);
                } catch (IOException e) {
                    // Should never happen
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    /**
     * ProtocolCodecFactory which allows to test AAAA resolution
     */
    private final class TestDnsProtocolUdpCodecFactory implements ProtocolCodecFactory {
        @Override
        public ProtocolEncoder getEncoder(IoSession session) throws Exception {
            return new DnsUdpEncoder() {

                @Override
                public void encode(IoSession session, Object message, ProtocolEncoderOutput out) {
                    IoBuffer buf = IoBuffer.allocate(1024);
                    FakeDNSServer.this.encode((DnsMessage) message, buf);
                    buf.flip();
                    out.write(buf);
                }
            };
        }

        @Override
        public ProtocolDecoder getDecoder(IoSession session) throws Exception {
            return new DnsUdpDecoder();
        }
    }

    /**
     * ProtocolCodecFactory which allows to test AAAA resolution
     */
    private final class TestDnsProtocolTcpCodecFactory implements ProtocolCodecFactory {
        @Override
        public ProtocolEncoder getEncoder(IoSession session) throws Exception {
            return new DnsUdpEncoder() {

                @Override
                public void encode(IoSession session, Object message, ProtocolEncoderOutput out) {
                    IoBuffer buf = IoBuffer.allocate(1024);
                    buf.putShort((short) 0);
                    FakeDNSServer.this.encode((DnsMessage) message, buf);
                    encoder.encode(buf, (DnsMessage) message);
                    int end = buf.position();
                    short recordLength = (short) (end - 2);
                    buf.rewind();
                    buf.putShort(recordLength);
                    buf.position(end);
                    buf.flip();
                    out.write(buf);
                }
            };
        }

        @Override
        public ProtocolDecoder getDecoder(IoSession session) throws Exception {
            return new DnsTcpDecoder();
        }
    }

    public void addRecordsToStore(String domainName, String... entries) {
        Set<ResourceRecord> records = new LinkedHashSet<>();
        Function<String, ResourceRecord> createRecord = ipAddress -> new VertxResourceRecord(domainName, ipAddress);
        for (String e : entries) {
            records.add(createRecord.apply(e));
        }
        store(x -> records);
    }
}
