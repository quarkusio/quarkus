package io.quarkus.redis.lettuce.runtime.internal;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.lettuce.core.protocol.CommandArgs;

public final class BlockingReads {

    private static final Set<IdentityKey> MARKED = ConcurrentHashMap.newKeySet();

    private static final ReferenceQueue<CommandArgs<?, ?>> CLEARED = new ReferenceQueue<>();

    private BlockingReads() {
        // Utility class
    }

    public static void mark(CommandArgs<?, ?> args) {
        expunge();
        MARKED.add(new IdentityKey(args, CLEARED));
    }

    public static boolean isMarked(CommandArgs<?, ?> args) {
        expunge();
        return args != null && MARKED.contains(new IdentityKey(args, null));
    }

    private static void expunge() {
        Reference<? extends CommandArgs<?, ?>> cleared;
        while ((cleared = CLEARED.poll()) != null) {
            MARKED.remove((IdentityKey) cleared);
        }
    }

    private static final class IdentityKey extends WeakReference<CommandArgs<?, ?>> {

        private final int hash;

        IdentityKey(CommandArgs<?, ?> referent, ReferenceQueue<CommandArgs<?, ?>> queue) {
            super(referent, queue);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof IdentityKey other)) {
                return false;
            }
            CommandArgs<?, ?> mine = get();
            return mine != null && mine == other.get();
        }

    }

}
