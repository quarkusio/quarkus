package io.quarkus.dev.console;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Formatter;
import java.util.Locale;

public class RedirectPrintStream extends PrintStream {

    private Formatter formatter;

    public RedirectPrintStream() {
        super(new ByteArrayOutputStream(0)); // never used
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        QuarkusConsole.INSTANCE.write(buf, off, len);
    }

    void write(String s) {
        QuarkusConsole.INSTANCE.write(s);
    }

    @Override
    public void write(int b) {
        write(new byte[] { (byte) b });
    }

    //@Overide
    public void write(byte[] buf) {
        write(buf, 0, buf.length);
    }

    //@Override
    public void writeBytes(byte[] buf) {
        write(buf, 0, buf.length);
    }

    @Override
    public void print(boolean b) {
        write(String.valueOf(b));
    }

    @Override
    public void print(char c) {
        write(String.valueOf(c));
    }

    @Override
    public void print(int i) {
        write(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        write(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        write(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        write(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        write(String.valueOf(s));
    }

    @Override
    public void print(String s) {
        write(String.valueOf(s));
    }

    @Override
    public void print(Object obj) {
        write(String.valueOf(obj));
    }

    @Override
    public void println() {
        write("\n");
    }

    @Override
    public void println(boolean x) {
        write(String.valueOf(x) + "\n");
    }

    @Override
    public void println(char x) {
        write(String.valueOf(x) + "\n");
    }

    @Override
    public void println(int x) {
        write(String.valueOf(x) + "\n");
    }

    @Override
    public void println(long x) {
        write(String.valueOf(x) + "\n");
    }

    @Override
    public void println(float x) {
        write(String.valueOf(x) + "\n");
    }

    @Override
    public void println(double x) {
        write(String.valueOf(x) + "\n");
    }

    @Override
    public void println(char[] x) {
        write(String.valueOf(x) + "\n");
    }

    @Override
    public void println(String x) {
        write(String.valueOf(x) + "\n");
    }

    @Override
    public void println(Object x) {
        write(String.valueOf(x) + "\n");
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        return format(format, args);
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        return format(l, format, args);
    }

    @Override
    public PrintStream format(String format, Object... args) {
        synchronized (this) {
            if ((formatter == null)
                    || (formatter.locale() != Locale.getDefault(Locale.Category.FORMAT)))
                formatter = new Formatter((Appendable) this);
            formatter.format(Locale.getDefault(Locale.Category.FORMAT),
                    format, args);
        }
        return this;
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        synchronized (this) {
            if ((formatter == null)
                    || (formatter.locale() != l))
                formatter = new Formatter(this, l);
            formatter.format(l, format, args);
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq) {
        print(String.valueOf(csq));
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        if (csq == null)
            csq = "null";
        return append(csq.subSequence(start, end));
    }

    @Override
    public PrintStream append(char c) {
        print(c);
        return this;
    }
}
