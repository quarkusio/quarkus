package io.quarkus.vertx.utils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.impl.Arguments;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A variant of {@link BufferImpl} doing no bound checks for performance reasons.
 */
public class NoBoundChecksBuffer implements Buffer {

    private ByteBuf buffer;

    public NoBoundChecksBuffer(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public String toString() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Override
    public String toString(String enc) {
        return buffer.toString(Charset.forName(enc));
    }

    @Override
    public String toString(Charset enc) {
        return buffer.toString(enc);
    }

    @Override
    public JsonObject toJsonObject() {
        return new JsonObject(this);
    }

    @Override
    public JsonArray toJsonArray() {
        return new JsonArray(this);
    }

    @Override
    public byte getByte(int pos) {
        return buffer.getByte(pos);
    }

    @Override
    public short getUnsignedByte(int pos) {
        return buffer.getUnsignedByte(pos);
    }

    @Override
    public int getInt(int pos) {
        return buffer.getInt(pos);
    }

    @Override
    public int getIntLE(int pos) {
        return buffer.getIntLE(pos);
    }

    @Override
    public long getUnsignedInt(int pos) {
        return buffer.getUnsignedInt(pos);
    }

    @Override
    public long getUnsignedIntLE(int pos) {
        return buffer.getUnsignedIntLE(pos);
    }

    @Override
    public long getLong(int pos) {
        return buffer.getLong(pos);
    }

    @Override
    public long getLongLE(int pos) {
        return buffer.getLongLE(pos);
    }

    @Override
    public double getDouble(int pos) {
        return buffer.getDouble(pos);
    }

    @Override
    public float getFloat(int pos) {
        return buffer.getFloat(pos);
    }

    @Override
    public short getShort(int pos) {
        return buffer.getShort(pos);
    }

    @Override
    public short getShortLE(int pos) {
        return buffer.getShortLE(pos);
    }

    @Override
    public int getUnsignedShort(int pos) {
        return buffer.getUnsignedShort(pos);
    }

    @Override
    public int getUnsignedShortLE(int pos) {
        return buffer.getUnsignedShortLE(pos);
    }

    @Override
    public int getMedium(int pos) {
        return buffer.getMedium(pos);
    }

    @Override
    public int getMediumLE(int pos) {
        return buffer.getMediumLE(pos);
    }

    @Override
    public int getUnsignedMedium(int pos) {
        return buffer.getUnsignedMedium(pos);
    }

    @Override
    public int getUnsignedMediumLE(int pos) {
        return buffer.getUnsignedMediumLE(pos);
    }

    @Override
    public byte[] getBytes() {
        byte[] arr = new byte[buffer.writerIndex()];
        buffer.getBytes(0, arr);
        return arr;
    }

    @Override
    public byte[] getBytes(int start, int end) {
        Arguments.require(end >= start, "end must be greater or equal than start");
        byte[] arr = new byte[end - start];
        buffer.getBytes(start, arr, 0, end - start);
        return arr;
    }

    @Override
    public Buffer getBytes(byte[] dst) {
        return getBytes(dst, 0);
    }

    @Override
    public Buffer getBytes(byte[] dst, int dstIndex) {
        return getBytes(0, buffer.writerIndex(), dst, dstIndex);
    }

    @Override
    public Buffer getBytes(int start, int end, byte[] dst) {
        return getBytes(start, end, dst, 0);
    }

    @Override
    public Buffer getBytes(int start, int end, byte[] dst, int dstIndex) {
        Arguments.require(end >= start, "end must be greater or equal than start");
        buffer.getBytes(start, dst, dstIndex, end - start);
        return this;
    }

    @Override
    public Buffer getBuffer(int start, int end) {
        return new NoBoundChecksBuffer(Unpooled.wrappedBuffer(getBytes(start, end)));
    }

    @Override
    public String getString(int start, int end, String enc) {
        byte[] bytes = getBytes(start, end);
        Charset cs = Charset.forName(enc);
        return new String(bytes, cs);
    }

    @Override
    public String getString(int start, int end) {
        byte[] bytes = getBytes(start, end);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public Buffer appendBuffer(Buffer buff) {
        buffer.writeBytes(buff.getByteBuf());
        return this;
    }

    @Override
    public Buffer appendBuffer(Buffer buff, int offset, int len) {
        ByteBuf byteBuf = buff.getByteBuf();
        int from = byteBuf.readerIndex() + offset;
        buffer.writeBytes(byteBuf, from, len);
        return this;
    }

    @Override
    public Buffer appendBytes(byte[] bytes) {
        buffer.writeBytes(bytes);
        return this;
    }

    @Override
    public Buffer appendBytes(byte[] bytes, int offset, int len) {
        buffer.writeBytes(bytes, offset, len);
        return this;
    }

    @Override
    public Buffer appendByte(byte b) {
        buffer.writeByte(b);
        return this;
    }

    @Override
    public Buffer appendUnsignedByte(short b) {
        buffer.writeByte(b);
        return this;
    }

    @Override
    public Buffer appendInt(int i) {
        buffer.writeInt(i);
        return this;
    }

    @Override
    public Buffer appendIntLE(int i) {
        buffer.writeIntLE(i);
        return this;
    }

    @Override
    public Buffer appendUnsignedInt(long i) {
        buffer.writeInt((int) i);
        return this;
    }

    @Override
    public Buffer appendUnsignedIntLE(long i) {
        buffer.writeIntLE((int) i);
        return this;
    }

    @Override
    public Buffer appendMedium(int i) {
        buffer.writeMedium(i);
        return this;
    }

    @Override
    public Buffer appendMediumLE(int i) {
        buffer.writeMediumLE(i);
        return this;
    }

    @Override
    public Buffer appendLong(long l) {
        buffer.writeLong(l);
        return this;
    }

    @Override
    public Buffer appendLongLE(long l) {
        buffer.writeLongLE(l);
        return this;
    }

    @Override
    public Buffer appendShort(short s) {
        buffer.writeShort(s);
        return this;
    }

    @Override
    public Buffer appendShortLE(short s) {
        buffer.writeShortLE(s);
        return this;
    }

    @Override
    public Buffer appendUnsignedShort(int s) {
        buffer.writeShort(s);
        return this;
    }

    @Override
    public Buffer appendUnsignedShortLE(int s) {
        buffer.writeShortLE(s);
        return this;
    }

    @Override
    public Buffer appendFloat(float f) {
        buffer.writeFloat(f);
        return this;
    }

    @Override
    public Buffer appendDouble(double d) {
        buffer.writeDouble(d);
        return this;
    }

    @Override
    public Buffer appendString(String str, String enc) {
        return append(str, Charset.forName(Objects.requireNonNull(enc)));
    }

    @Override
    public Buffer appendString(String str) {
        return append(str, CharsetUtil.UTF_8);
    }

    @Override
    public Buffer setByte(int pos, byte b) {
        ensureWritable(pos, 1);
        buffer.setByte(pos, b);
        return this;
    }

    @Override
    public Buffer setUnsignedByte(int pos, short b) {
        ensureWritable(pos, 1);
        buffer.setByte(pos, b);
        return this;
    }

    @Override
    public Buffer setInt(int pos, int i) {
        ensureWritable(pos, 4);
        buffer.setInt(pos, i);
        return this;
    }

    @Override
    public Buffer setIntLE(int pos, int i) {
        ensureWritable(pos, 4);
        buffer.setIntLE(pos, i);
        return this;
    }

    @Override
    public Buffer setUnsignedInt(int pos, long i) {
        ensureWritable(pos, 4);
        buffer.setInt(pos, (int) i);
        return this;
    }

    @Override
    public Buffer setUnsignedIntLE(int pos, long i) {
        ensureWritable(pos, 4);
        buffer.setIntLE(pos, (int) i);
        return this;
    }

    @Override
    public Buffer setMedium(int pos, int i) {
        ensureWritable(pos, 3);
        buffer.setMedium(pos, i);
        return this;
    }

    @Override
    public Buffer setMediumLE(int pos, int i) {
        ensureWritable(pos, 3);
        buffer.setMediumLE(pos, i);
        return this;
    }

    @Override
    public Buffer setLong(int pos, long l) {
        ensureWritable(pos, 8);
        buffer.setLong(pos, l);
        return this;
    }

    @Override
    public Buffer setLongLE(int pos, long l) {
        ensureWritable(pos, 8);
        buffer.setLongLE(pos, l);
        return this;
    }

    @Override
    public Buffer setDouble(int pos, double d) {
        ensureWritable(pos, 8);
        buffer.setDouble(pos, d);
        return this;
    }

    @Override
    public Buffer setFloat(int pos, float f) {
        ensureWritable(pos, 4);
        buffer.setFloat(pos, f);
        return this;
    }

    @Override
    public Buffer setShort(int pos, short s) {
        ensureWritable(pos, 2);
        buffer.setShort(pos, s);
        return this;
    }

    @Override
    public Buffer setShortLE(int pos, short s) {
        ensureWritable(pos, 2);
        buffer.setShortLE(pos, s);
        return this;
    }

    @Override
    public Buffer setUnsignedShort(int pos, int s) {
        ensureWritable(pos, 2);
        buffer.setShort(pos, s);
        return this;
    }

    @Override
    public Buffer setUnsignedShortLE(int pos, int s) {
        ensureWritable(pos, 2);
        buffer.setShortLE(pos, s);
        return this;
    }

    @Override
    public Buffer setBuffer(int pos, Buffer b) {
        ensureWritable(pos, b.length());
        buffer.setBytes(pos, b.getByteBuf());
        return this;
    }

    @Override
    public Buffer setBuffer(int pos, Buffer b, int offset, int len) {
        ensureWritable(pos, len);
        ByteBuf byteBuf = b.getByteBuf();
        buffer.setBytes(pos, byteBuf, byteBuf.readerIndex() + offset, len);
        return this;
    }

    @Override
    public NoBoundChecksBuffer setBytes(int pos, ByteBuffer b) {
        ensureWritable(pos, b.limit());
        buffer.setBytes(pos, b);
        return this;
    }

    @Override
    public Buffer setBytes(int pos, byte[] b) {
        ensureWritable(pos, b.length);
        buffer.setBytes(pos, b);
        return this;
    }

    @Override
    public Buffer setBytes(int pos, byte[] b, int offset, int len) {
        ensureWritable(pos, len);
        buffer.setBytes(pos, b, offset, len);
        return this;
    }

    @Override
    public Buffer setString(int pos, String str) {
        return setBytes(pos, str, CharsetUtil.UTF_8);
    }

    @Override
    public Buffer setString(int pos, String str, String enc) {
        return setBytes(pos, str, Charset.forName(enc));
    }

    @Override
    public int length() {
        return buffer.writerIndex();
    }

    @Override
    public Buffer copy() {
        return new NoBoundChecksBuffer(buffer.copy());
    }

    @Override
    public Buffer slice() {
        return new NoBoundChecksBuffer(buffer.slice());
    }

    @Override
    public Buffer slice(int start, int end) {
        return new NoBoundChecksBuffer(buffer.slice(start, end - start));
    }

    @Override
    public ByteBuf getByteBuf() {
        // Return a duplicate so the Buffer can be written multiple times.
        // See #648
        return buffer;
    }

    private Buffer append(String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        buffer.writeBytes(bytes);
        return this;
    }

    private Buffer setBytes(int pos, String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        ensureWritable(pos, bytes.length);
        buffer.setBytes(pos, bytes);
        return this;
    }

    private void ensureWritable(int pos, int len) {
        int ni = pos + len;
        int cap = buffer.capacity();
        int over = ni - cap;
        if (over > 0) {
            buffer.writerIndex(cap);
            buffer.ensureWritable(over);
        }
        //We have to make sure that the writerindex is always positioned on the last bit of data set in the buffer
        if (ni > buffer.writerIndex()) {
            buffer.writerIndex(ni);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NoBoundChecksBuffer buffer1 = (NoBoundChecksBuffer) o;
        return buffer != null ? buffer.equals(buffer1.buffer) : buffer1.buffer == null;
    }

    @Override
    public int hashCode() {
        return buffer != null ? buffer.hashCode() : 0;
    }

    @Override
    public void writeToBuffer(Buffer buff) {
        buff.appendInt(this.length());
        buff.appendBuffer(this);
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        int len = buffer.getInt(pos);
        Buffer b = buffer.getBuffer(pos + 4, pos + 4 + len);
        this.buffer = b.getByteBuf();
        return pos + 4 + len;
    }
}
