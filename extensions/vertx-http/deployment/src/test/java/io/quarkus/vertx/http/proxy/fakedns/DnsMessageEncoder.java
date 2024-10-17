package io.quarkus.vertx.http.proxy.fakedns;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
import org.apache.directory.server.dns.io.encoder.*;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.i18n.I18n;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An encoder for DNS messages. The primary usage of the DnsMessageEncoder is
 * to call the <code>encode(ByteBuffer, DnsMessage)</code> method which will
 * write the message to the outgoing ByteBuffer according to the DnsMessage
 * encoding in RFC-1035.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DnsMessageEncoder {
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger(DnsMessageEncoder.class);

    /**
     * A Hashed Adapter mapping record types to their encoders.
     */
    private static final Map<RecordType, RecordEncoder> DEFAULT_ENCODERS;

    static {
        Map<RecordType, RecordEncoder> map = new HashMap<RecordType, RecordEncoder>();

        map.put(RecordType.SOA, new StartOfAuthorityRecordEncoder());
        map.put(RecordType.A, new AddressRecordEncoder());
        map.put(RecordType.NS, new NameServerRecordEncoder());
        map.put(RecordType.CNAME, new CanonicalNameRecordEncoder());
        map.put(RecordType.PTR, new PointerRecordEncoder());
        map.put(RecordType.MX, new MailExchangeRecordEncoder());
        map.put(RecordType.SRV, new ServerSelectionRecordEncoder());
        map.put(RecordType.TXT, new TextRecordEncoder());
        map.put(RecordType.DNAME, new DnameRecordEncoder());

        DEFAULT_ENCODERS = Collections.unmodifiableMap(map);
    }

    /**
     * Encodes the {@link DnsMessage} into the {@link IoBuffer}.
     *
     * @param byteBuffer
     * @param message
     */
    public void encode(IoBuffer byteBuffer, DnsMessage message) {
        byteBuffer.putShort((short) message.getTransactionId());

        byte header = (byte) 0x00;
        header |= encodeMessageType(message.getMessageType());
        header |= encodeOpCode(message.getOpCode());
        header |= encodeAuthoritativeAnswer(message.isAuthoritativeAnswer());
        header |= encodeTruncated(message.isTruncated());
        header |= encodeRecursionDesired(message.isRecursionDesired());
        byteBuffer.put(header);

        header = (byte) 0x00;
        header |= encodeRecursionAvailable(message.isRecursionAvailable());
        header |= encodeResponseCode(message.getResponseCode());
        byteBuffer.put(header);

        byteBuffer
                .putShort((short) (message.getQuestionRecords() != null ? message.getQuestionRecords().size() : 0));
        byteBuffer.putShort((short) (message.getAnswerRecords() != null ? message.getAnswerRecords().size() : 0));
        byteBuffer.putShort((short) (message.getAuthorityRecords() != null ? message.getAuthorityRecords().size()
                : 0));
        byteBuffer.putShort((short) (message.getAdditionalRecords() != null ? message.getAdditionalRecords().size()
                : 0));

        putQuestionRecords(byteBuffer, message.getQuestionRecords());
        putResourceRecords(byteBuffer, message.getAnswerRecords());
        putResourceRecords(byteBuffer, message.getAuthorityRecords());
        putResourceRecords(byteBuffer, message.getAdditionalRecords());
    }

    private void putQuestionRecords(IoBuffer byteBuffer, List<QuestionRecord> questions) {
        if (questions == null) {
            return;
        }

        QuestionRecordEncoder encoder = new QuestionRecordEncoder();

        Iterator<QuestionRecord> it = questions.iterator();

        while (it.hasNext()) {
            QuestionRecord question = it.next();
            encoder.put(byteBuffer, question);
        }
    }

    private void putResourceRecords(IoBuffer byteBuffer, List<ResourceRecord> records) {
        if (records == null) {
            return;
        }

        Iterator<ResourceRecord> it = records.iterator();

        while (it.hasNext()) {
            ResourceRecord record = it.next();

            try {
                put(byteBuffer, record);
            } catch (IOException ioe) {
                log.error(ioe.getLocalizedMessage(), ioe);
            }
        }
    }

    private void put(IoBuffer byteBuffer, ResourceRecord record) throws IOException {
        RecordType type = record.getRecordType();

        RecordEncoder encoder = DEFAULT_ENCODERS.get(type);

        if (encoder == null) {
            throw new IOException(I18n.err(I18n.ERR_597, type));
        }

        encoder.put(byteBuffer, record);
    }

    private byte encodeMessageType(MessageType messageType) {
        byte oneBit = (byte) (messageType.convert() & 0x01);
        return (byte) (oneBit << 7);
    }

    private byte encodeOpCode(OpCode opCode) {
        byte fourBits = (byte) (opCode.convert() & 0x0F);
        return (byte) (fourBits << 3);
    }

    private byte encodeAuthoritativeAnswer(boolean authoritative) {
        if (authoritative) {
            return (byte) ((byte) 0x01 << 2);
        }
        return (byte) 0;
    }

    private byte encodeTruncated(boolean truncated) {
        if (truncated) {
            return (byte) ((byte) 0x01 << 1);
        }
        return 0;
    }

    private byte encodeRecursionDesired(boolean recursionDesired) {
        if (recursionDesired) {
            return (byte) 0x01;
        }
        return 0;
    }

    private byte encodeRecursionAvailable(boolean recursionAvailable) {
        if (recursionAvailable) {
            return (byte) ((byte) 0x01 << 7);
        }
        return 0;
    }

    private byte encodeResponseCode(ResponseCode responseCode) {
        return (byte) (responseCode.convert() & 0x0F);
    }
}
