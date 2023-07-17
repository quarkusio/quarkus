package io.quarkus.vertx.core.runtime;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.Arguments;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class VertxBufferImpl implements Buffer {

    private ByteBuf buffer;

    public VertxBufferImpl(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public String toString() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    public String toString(String enc) {
        return buffer.toString(Charset.forName(enc));
    }

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

    public byte getByte(int pos) {
        return buffer.getByte(pos);
    }

    public short getUnsignedByte(int pos) {
        return buffer.getUnsignedByte(pos);
    }

    public int getInt(int pos) {
        return buffer.getInt(pos);
    }

    public int getIntLE(int pos) {
        return buffer.getIntLE(pos);
    }

    public long getUnsignedInt(int pos) {
        return buffer.getUnsignedInt(pos);
    }

    public long getUnsignedIntLE(int pos) {
        return buffer.getUnsignedIntLE(pos);
    }

    public long getLong(int pos) {
        return buffer.getLong(pos);
    }

    public long getLongLE(int pos) {
        return buffer.getLongLE(pos);
    }

    public double getDouble(int pos) {
        return buffer.getDouble(pos);
    }

    public float getFloat(int pos) {
        return buffer.getFloat(pos);
    }

    public short getShort(int pos) {
        return buffer.getShort(pos);
    }

    public short getShortLE(int pos) {
        return buffer.getShortLE(pos);
    }

    public int getUnsignedShort(int pos) {
        return buffer.getUnsignedShort(pos);
    }

    public int getUnsignedShortLE(int pos) {
        return buffer.getUnsignedShortLE(pos);
    }

    public int getMedium(int pos) {
        return buffer.getMedium(pos);
    }

    public int getMediumLE(int pos) {
        return buffer.getMediumLE(pos);
    }

    public int getUnsignedMedium(int pos) {
        return buffer.getUnsignedMedium(pos);
    }

    public int getUnsignedMediumLE(int pos) {
        return buffer.getUnsignedMediumLE(pos);
    }

    public byte[] getBytes() {
        byte[] arr = new byte[buffer.writerIndex()];
        buffer.getBytes(0, arr);
        return arr;
    }

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

    public Buffer getBuffer(int start, int end) {
        return new VertxBufferImpl(Unpooled.wrappedBuffer(getBytes(start, end)));
    }

    public String getString(int start, int end, String enc) {
        byte[] bytes = getBytes(start, end);
        Charset cs = Charset.forName(enc);
        return new String(bytes, cs);
    }

    public String getString(int start, int end) {
        byte[] bytes = getBytes(start, end);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public Buffer appendBuffer(Buffer buff) {
        buffer.writeBytes(buff.getByteBuf());
        return this;
    }

    public Buffer appendBuffer(Buffer buff, int offset, int len) {
        ByteBuf byteBuf = buff.getByteBuf();
        int from = byteBuf.readerIndex() + offset;
        buffer.writeBytes(byteBuf, from, len);
        return this;
    }

    public Buffer appendBytes(byte[] bytes) {
        buffer.writeBytes(bytes);
        return this;
    }

    public Buffer appendBytes(byte[] bytes, int offset, int len) {
        buffer.writeBytes(bytes, offset, len);
        return this;
    }

    public Buffer appendByte(byte b) {
        buffer.writeByte(b);
        return this;
    }

    public Buffer appendUnsignedByte(short b) {
        buffer.writeByte(b);
        return this;
    }

    public Buffer appendInt(int i) {
        buffer.writeInt(i);
        return this;
    }

    public Buffer appendIntLE(int i) {
        buffer.writeIntLE(i);
        return this;
    }

    public Buffer appendUnsignedInt(long i) {
        buffer.writeInt((int) i);
        return this;
    }

    public Buffer appendUnsignedIntLE(long i) {
        buffer.writeIntLE((int) i);
        return this;
    }

    public Buffer appendMedium(int i) {
        buffer.writeMedium(i);
        return this;
    }

    public Buffer appendMediumLE(int i) {
        buffer.writeMediumLE(i);
        return this;
    }

    public Buffer appendLong(long l) {
        buffer.writeLong(l);
        return this;
    }

    public Buffer appendLongLE(long l) {
        buffer.writeLongLE(l);
        return this;
    }

    public Buffer appendShort(short s) {
        buffer.writeShort(s);
        return this;
    }

    public Buffer appendShortLE(short s) {
        buffer.writeShortLE(s);
        return this;
    }

    public Buffer appendUnsignedShort(int s) {
        buffer.writeShort(s);
        return this;
    }

    public Buffer appendUnsignedShortLE(int s) {
        buffer.writeShortLE(s);
        return this;
    }

    public Buffer appendFloat(float f) {
        buffer.writeFloat(f);
        return this;
    }

    public Buffer appendDouble(double d) {
        buffer.writeDouble(d);
        return this;
    }

    public Buffer appendString(String str, String enc) {
        return append(str, Charset.forName(Objects.requireNonNull(enc)));
    }

    public Buffer appendString(String str) {
        return append(str, CharsetUtil.UTF_8);
    }

    public Buffer setByte(int pos, byte b) {
        ensureWritable(pos, 1);
        buffer.setByte(pos, b);
        return this;
    }

    public Buffer setUnsignedByte(int pos, short b) {
        ensureWritable(pos, 1);
        buffer.setByte(pos, b);
        return this;
    }

    public Buffer setInt(int pos, int i) {
        ensureWritable(pos, 4);
        buffer.setInt(pos, i);
        return this;
    }

    public Buffer setIntLE(int pos, int i) {
        ensureWritable(pos, 4);
        buffer.setIntLE(pos, i);
        return this;
    }

    public Buffer setUnsignedInt(int pos, long i) {
        ensureWritable(pos, 4);
        buffer.setInt(pos, (int) i);
        return this;
    }

    public Buffer setUnsignedIntLE(int pos, long i) {
        ensureWritable(pos, 4);
        buffer.setIntLE(pos, (int) i);
        return this;
    }

    public Buffer setMedium(int pos, int i) {
        ensureWritable(pos, 3);
        buffer.setMedium(pos, i);
        return this;
    }

    public Buffer setMediumLE(int pos, int i) {
        ensureWritable(pos, 3);
        buffer.setMediumLE(pos, i);
        return this;
    }

    public Buffer setLong(int pos, long l) {
        ensureWritable(pos, 8);
        buffer.setLong(pos, l);
        return this;
    }

    public Buffer setLongLE(int pos, long l) {
        ensureWritable(pos, 8);
        buffer.setLongLE(pos, l);
        return this;
    }

    public Buffer setDouble(int pos, double d) {
        ensureWritable(pos, 8);
        buffer.setDouble(pos, d);
        return this;
    }

    public Buffer setFloat(int pos, float f) {
        ensureWritable(pos, 4);
        buffer.setFloat(pos, f);
        return this;
    }

    public Buffer setShort(int pos, short s) {
        ensureWritable(pos, 2);
        buffer.setShort(pos, s);
        return this;
    }

    public Buffer setShortLE(int pos, short s) {
        ensureWritable(pos, 2);
        buffer.setShortLE(pos, s);
        return this;
    }

    public Buffer setUnsignedShort(int pos, int s) {
        ensureWritable(pos, 2);
        buffer.setShort(pos, s);
        return this;
    }

    public Buffer setUnsignedShortLE(int pos, int s) {
        ensureWritable(pos, 2);
        buffer.setShortLE(pos, s);
        return this;
    }

    public Buffer setBuffer(int pos, Buffer b) {
        ensureWritable(pos, b.length());
        buffer.setBytes(pos, b.getByteBuf());
        return this;
    }

    public Buffer setBuffer(int pos, Buffer b, int offset, int len) {
        ensureWritable(pos, len);
        ByteBuf byteBuf = b.getByteBuf();
        buffer.setBytes(pos, byteBuf, byteBuf.readerIndex() + offset, len);
        return this;
    }

    public VertxBufferImpl setBytes(int pos, ByteBuffer b) {
        ensureWritable(pos, b.limit());
        buffer.setBytes(pos, b);
        return this;
    }

    public Buffer setBytes(int pos, byte[] b) {
        ensureWritable(pos, b.length);
        buffer.setBytes(pos, b);
        return this;
    }

    public Buffer setBytes(int pos, byte[] b, int offset, int len) {
        ensureWritable(pos, len);
        buffer.setBytes(pos, b, offset, len);
        return this;
    }

    public Buffer setString(int pos, String str) {
        return setBytes(pos, str, CharsetUtil.UTF_8);
    }

    public Buffer setString(int pos, String str, String enc) {
        return setBytes(pos, str, Charset.forName(enc));
    }

    public int length() {
        return buffer.writerIndex();
    }

    public Buffer copy() {
        return new VertxBufferImpl(buffer.copy());
    }

    public Buffer slice() {
        return new VertxBufferImpl(buffer.slice());
    }

    public Buffer slice(int start, int end) {
        return new VertxBufferImpl(buffer.slice(start, end - start));
    }

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
        VertxBufferImpl buffer1 = (VertxBufferImpl) o;
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
