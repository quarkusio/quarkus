package io.quarkus.runtime.logging;

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Level;

import org.jboss.logmanager.LogContext;

import io.quarkus.runtime.ObjectSubstitution;

/**
 * A level that may be inheritable.
 */
public abstract class InheritableLevel implements Constable {

    /** The descriptor of this class, used to describe instances as dynamic constants. */
    private static final ClassDesc CD_INHERITABLE_LEVEL = ClassDesc.of("io.quarkus.runtime.logging.InheritableLevel");

    /** A method handle descriptor for {@link #of(String)}, used to reconstruct instances as dynamic constants. */
    private static final DirectMethodHandleDesc OF_STRING = MethodHandleDesc.ofMethod(
            DirectMethodHandleDesc.Kind.STATIC, CD_INHERITABLE_LEVEL, "of",
            MethodTypeDesc.of(CD_INHERITABLE_LEVEL, ConstantDescs.CD_String));

    InheritableLevel() {
    }

    public static InheritableLevel of(String str) {
        if (str.equalsIgnoreCase("inherit")) {
            return Inherited.INSTANCE;
        } else {
            return of(LogContext.getLogContext().getLevelForName(str.toUpperCase(Locale.ROOT)));
        }
    }

    public static InheritableLevel of(Level level) {
        return new ActualLevel(level);
    }

    public abstract boolean isInherited();

    public abstract Level getLevel();

    public abstract String toString();

    public final boolean equals(Object obj) {
        return obj instanceof InheritableLevel && equals((InheritableLevel) obj);
    }

    public abstract boolean equals(InheritableLevel other);

    public abstract int hashCode();

    /**
     * {@return a description of this level as a dynamic constant} The constant reconstructs the level at
     * resolution time by invoking {@link #of(String)} with {@link #describeArg()}, which allows an
     * {@code InheritableLevel} to be captured directly (for example, in a service action).
     */
    @Override
    public final Optional<? extends ConstantDesc> describeConstable() {
        return Optional.of(DynamicConstantDesc.ofNamed(ConstantDescs.BSM_INVOKE, "level", CD_INHERITABLE_LEVEL,
                OF_STRING, describeArg()));
    }

    /**
     * {@return the string that, when passed to {@link #of(String)}, reconstructs this level}
     */
    abstract String describeArg();

    public static final class ActualLevel extends InheritableLevel {
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

        @Override
        String describeArg() {
            return level.toString();
        }

        public boolean equals(final InheritableLevel other) {
            return other instanceof ActualLevel && level.equals(((ActualLevel) other).level);
        }

        public int hashCode() {
            return level.hashCode();
        }
    }

    public static final class Inherited extends InheritableLevel {
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

        @Override
        String describeArg() {
            return "inherit";
        }

        public boolean equals(final InheritableLevel other) {
            return other instanceof Inherited;
        }

        public int hashCode() {
            return 0;
        }
    }

    public static class Substitution implements ObjectSubstitution<InheritableLevel, String> {

        @Override
        public String serialize(InheritableLevel obj) {
            return obj.toString();
        }

        @Override
        public InheritableLevel deserialize(String obj) {
            return InheritableLevel.of(obj);
        }
    }
}
