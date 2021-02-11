package io.quarkus.runtime.logging;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import org.jboss.logmanager.LogContext;

/**
 * A level that may be inheritable.
 */
public abstract class InheritableLevel {
    InheritableLevel() {
    }

    public static InheritableLevel of(String str) {
        if (str.equalsIgnoreCase("inherit")) {
            return Inherited.INSTANCE;
        } else {
            return new ActualLevel(LogContext.getLogContext().getLevelForName(str.toUpperCase(Locale.ROOT)));
        }
    }

    public abstract boolean isInherited();

    public abstract Level getLevel();

    public abstract String toString();

    public final boolean equals(Object obj) {
        return obj instanceof InheritableLevel && equals((InheritableLevel) obj);
    }

    public abstract boolean equals(InheritableLevel other);

    public abstract int hashCode();

    static final class ActualLevel extends InheritableLevel {
        final Level level;

        ActualLevel(Level level) {
            this.level = level;
        }

        public boolean isInherited() {
            return false;
        }

        public Level getLevel() {
            return level;
        }

        public String toString() {
            return level.toString();
        }

        public boolean equals(final InheritableLevel other) {
            return other instanceof ActualLevel && level.equals(((ActualLevel) other).level);
        }

        public int hashCode() {
            return level.hashCode();
        }
    }

    static final class Inherited extends InheritableLevel {
        static final Inherited INSTANCE = new Inherited();

        private Inherited() {
        }

        public boolean isInherited() {
            return true;
        }

        public Level getLevel() {
            throw new NoSuchElementException();
        }

        public String toString() {
            return "inherited";
        }

        public boolean equals(final InheritableLevel other) {
            return other instanceof Inherited;
        }

        public int hashCode() {
            return 0;
        }
    }
}
