package io.quarkus.example.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.transaction.RollbackException;

import org.hibernate.*;
import org.hibernate.cache.spi.Region;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.infinispan.quarkus.hibernate.cache.PutFromLoadValidator;
import org.jboss.logging.Logger;

import io.quarkus.example.infinispancachejpa.correctness.Family;
import io.quarkus.example.infinispancachejpa.correctness.Member;

public class InfinispanCacheJPACorrectnessTestCase {
    private static final Logger log = Logger.getLogger(InfinispanCacheJPACorrectnessTestCase.class);

    static final long EXECUTION_TIME = TimeUnit.MINUTES.toMillis(2);
    static final int NUM_THREADS = 4;
    static final int NUM_FAMILIES = 1;
    static final int NUM_ACCESS_AFTER_REMOVAL = NUM_THREADS * 2;
    static final int MAX_MEMBERS = 10;
    final static Comparator<Log<?>> WALL_CLOCK_TIME_COMPARATOR = Comparator.comparingLong(o -> o.wallClockTime);

    private final static boolean INVALIDATE_REGION = true;
    private final static boolean INJECT_FAILURES = Boolean.getBoolean("testInfinispan.correctness.injectFailures");

    final AtomicInteger timestampGenerator = new AtomicInteger();
    final ConcurrentSkipListMap<Integer, AtomicInteger> familyIds = new ConcurrentSkipListMap<>();
    volatile boolean running = true;

    final ThreadLocal<Map<Integer, List<Log<String>>>> familyNames = ThreadLocal.withInitial(HashMap::new);
    final ThreadLocal<Map<Integer, List<Log<Set<String>>>>> familyMembers = ThreadLocal.withInitial(HashMap::new);
    private BlockingDeque<Exception> exceptions = new LinkedBlockingDeque<>();

    private final static Class[][] EXPECTED = {
            { javax.persistence.RollbackException.class, PersistenceException.class, TransactionException.class,
                    RollbackException.class, StaleObjectStateException.class },
            { javax.persistence.RollbackException.class, PersistenceException.class, TransactionException.class,
                    RollbackException.class, PessimisticLockException.class },
            { TransactionException.class, RollbackException.class, LockAcquisitionException.class },
            { StaleStateException.class, PessimisticLockException.class },
            { StaleStateException.class, ObjectNotFoundException.class },
            { StaleStateException.class, ConstraintViolationException.class },
            { StaleStateException.class, LockAcquisitionException.class },
            { javax.persistence.RollbackException.class, PersistenceException.class, ConstraintViolationException.class },
            { PersistenceException.class, LockAcquisitionException.class },
            { javax.persistence.RollbackException.class, javax.persistence.PessimisticLockException.class,
                    PessimisticLockException.class },
            { javax.persistence.RollbackException.class, javax.persistence.OptimisticLockException.class,
                    StaleStateException.class },
            { PessimisticLockException.class },
            { StaleObjectStateException.class },
            { EntityNotFoundException.class },
            { LockAcquisitionException.class }
    };

    private final SessionFactory sessionFactory;
    private final Class<? extends Family> familyClass;
    private final Function<Family, ? extends Member> memberCtor;
    private final Supplier<Family> familyCtor;

    public InfinispanCacheJPACorrectnessTestCase(
            SessionFactory sessionFactory, Class<? extends Family> familyClass, Function<Family, ? extends Member> memberCtor,
            Supplier<Family> familyCtor) {
        this.sessionFactory = sessionFactory;
        this.familyClass = familyClass;
        this.memberCtor = memberCtor;
        this.familyCtor = familyCtor;
    }

    public void test() throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);

        Map<Integer, List<Log<String>>> allFamilyNames = new HashMap<>();
        Map<Integer, List<Log<Set<String>>>> allFamilyMembers = new HashMap<>();

        running = true;
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; ++i) {
            final int I = i;
            futures.add(exec.submit(() -> {
                Thread.currentThread().setName("Node" + (char) ('A') + "-thread-" + I);
                while (running) {
                    Operation operation;
                    if (familyIds.size() < NUM_FAMILIES) {
                        operation = new InsertFamily(ThreadLocalRandom.current().nextInt(5) == 0);
                    } else {
                        operation = getOperation();
                    }
                    try {
                        operation.run();
                    } catch (Exception e) {
                        // ignore exceptions from optimistic failures and induced exceptions
                        if (hasCause(e, InducedException.class)) {
                            continue;
                        } else if (Stream.of(EXPECTED).anyMatch(exceptions -> matches(e, exceptions))) {
                            continue;
                        }
                        exceptions.add(e);
                        log.error("Failed " + operation.getClass().getName(), e);
                    }
                }
                synchronized (allFamilyNames) {
                    for (Map.Entry<Integer, List<Log<String>>> entry : familyNames.get().entrySet()) {
                        List<Log<String>> list = allFamilyNames.get(entry.getKey());
                        if (list == null)
                            allFamilyNames.put(entry.getKey(), list = new ArrayList<>());
                        list.addAll(entry.getValue());
                    }
                    for (Map.Entry<Integer, List<Log<Set<String>>>> entry : familyMembers.get().entrySet()) {
                        List<Log<Set<String>>> list = allFamilyMembers.get(entry.getKey());
                        if (list == null)
                            allFamilyMembers.put(entry.getKey(), list = new ArrayList<>());
                        list.addAll(entry.getValue());
                    }
                }
                return null;
            }));
        }

        Exception failure = exceptions.poll(EXECUTION_TIME, TimeUnit.MILLISECONDS);
        if (failure != null)
            exceptions.addFirst(failure);
        running = false;
        exec.shutdown();
        if (!exec.awaitTermination(1000, TimeUnit.SECONDS))
            throw new IllegalStateException();
        for (Future<Void> f : futures) {
            f.get(); // check for exceptions
        }

        // TODO: do we really need this?
        checkForEmptyPendingPuts();

        log.infof("Generated %d timestamps%n", timestampGenerator.get());
        AtomicInteger created = new AtomicInteger();
        AtomicInteger removed = new AtomicInteger();
        ForkJoinPool threadPool = ForkJoinPool.commonPool();
        ArrayList<ForkJoinTask<?>> tasks = new ArrayList<>();
        for (Map.Entry<Integer, List<Log<String>>> entry : allFamilyNames.entrySet()) {
            tasks.add(threadPool.submit(() -> {
                int familyId = entry.getKey();
                List<Log<String>> list = entry.getValue();
                created.incrementAndGet();
                NavigableMap<Integer, List<Log<String>>> logByTime = getWritesAtTime(list);
                checkCorrectness("family_name-" + familyId + "-", list, logByTime);
                if (list.stream().anyMatch(l -> l.type == LogType.WRITE && l.getValue() == null)) {
                    removed.incrementAndGet();
                }
            }));
        }
        for (Map.Entry<Integer, List<Log<Set<String>>>> entry : allFamilyMembers.entrySet()) {
            tasks.add(threadPool.submit(() -> {
                int familyId = entry.getKey();
                List<Log<Set<String>>> list = entry.getValue();
                NavigableMap<Integer, List<Log<Set<String>>>> logByTime = getWritesAtTime(list);
                checkCorrectness("family_members-" + familyId + "-", list, logByTime);
            }));
        }
        for (ForkJoinTask<?> task : tasks) {
            // with heavy logging this may have trouble to complete
            task.get(30, TimeUnit.SECONDS);
        }
        if (!exceptions.isEmpty()) {
            for (Exception e : exceptions) {
                log.error("Test failure", e);
            }
            throw new IllegalStateException("There were " + exceptions.size() + " exceptions");
        }
        log.infof("Created %d families, removed %d%n", created.get(), removed.get());
    }

    private static class DelayedInvalidators {
        final ConcurrentMap map;
        final Object key;

        public DelayedInvalidators(ConcurrentMap map, Object key) {
            this.map = map;
            this.key = key;
        }

        public Object getPendingPutMap() {
            return map.get(key);
        }
    }

    protected void checkForEmptyPendingPuts() throws Exception {
        Field pp = PutFromLoadValidator.class.getDeclaredField("pendingPuts");
        pp.setAccessible(true);
        Method getInvalidators = null;
        List<DelayedInvalidators> delayed = new LinkedList<>();

        SessionFactoryImplementor sfi = (SessionFactoryImplementor) sessionFactory;
        for (Object regionName : sfi.getCache().getCacheRegionNames()) {
            PutFromLoadValidator validator = getPutFromLoadValidator(sfi, (String) regionName);
            if (validator == null) {
                log.warn("No validator for " + regionName);
                continue;
            }
            ConcurrentMap<Object, Object> map = ((com.github.benmanes.caffeine.cache.Cache) pp.get(validator)).asMap();
            for (Iterator<Map.Entry<Object, Object>> iterator = map.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = iterator.next();
                if (getInvalidators == null) {
                    getInvalidators = entry.getValue().getClass().getMethod("getInvalidators");
                    getInvalidators.setAccessible(true);
                }
                java.util.Collection invalidators = (java.util.Collection) getInvalidators.invoke(entry.getValue());
                if (invalidators != null && !invalidators.isEmpty()) {
                    delayed.add(new DelayedInvalidators(map, entry.getKey()));
                }
            }
        }

        // poll until all invalidations come
        long deadline = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < deadline) {
            iterateInvalidators(delayed, getInvalidators, (k, i) -> {
            });
            if (delayed.isEmpty()) {
                break;
            }
            Thread.sleep(1000);
        }
        if (!delayed.isEmpty()) {
            iterateInvalidators(delayed, getInvalidators, (k, i) -> log.warnf("Left invalidators on key %s: %s", k, i));
            throw new IllegalStateException("Invalidators were not cleared: " + delayed.size());
        }
    }

    private void iterateInvalidators(List<DelayedInvalidators> delayed, Method getInvalidators,
            BiConsumer<Object, java.util.Collection> invalidatorConsumer)
            throws IllegalAccessException, InvocationTargetException {
        for (Iterator<DelayedInvalidators> iterator = delayed.iterator(); iterator.hasNext();) {
            DelayedInvalidators entry = iterator.next();
            Object pendingPutMap = entry.getPendingPutMap();
            if (pendingPutMap == null) {
                iterator.remove();
            } else {
                java.util.Collection invalidators = (java.util.Collection) getInvalidators.invoke(pendingPutMap);
                if (invalidators == null || invalidators.isEmpty()) {
                    iterator.remove();
                }
                invalidatorConsumer.accept(entry.key, invalidators);
            }
        }
    }

    private PutFromLoadValidator getPutFromLoadValidator(SessionFactoryImplementor sfi, String regionName)
            throws NoSuchFieldException, IllegalAccessException {
        Region region = sfi.getCache().getRegion(regionName);
        if (region == null) {
            return null;
        }
        Field validatorField = getField(region.getClass(), "validator");
        if (validatorField == null) {
            return null;
        }

        Object validator = validatorField.get(region);
        if (validator == null) {
            return null;
        }

        // Non-null in strict data access patterns
        return (PutFromLoadValidator) validator;
    }

    private Field getField(Class<?> clazz, String fieldName) {
        Field f = null;
        while (clazz != null && clazz != Object.class) {
            try {
                f = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (f != null) {
            f.setAccessible(true);
        }
        return f;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> clazz) {
        if (throwable == null)
            return false;
        Throwable cause = throwable.getCause();
        if (throwable == cause)
            return false;
        if (clazz.isInstance(cause))
            return true;
        return hasCause(cause, clazz);
    }

    private boolean matches(Throwable throwable, Class[] classes) {
        return matches(throwable, classes, 0);
    }

    private boolean matches(Throwable throwable, Class[] classes, int index) {
        return index >= classes.length
                || (classes[index].isInstance(throwable)
                        && matches(throwable.getCause(), classes, index + 1));
    }

    private <T> NavigableMap<Integer, List<Log<T>>> getWritesAtTime(List<Log<T>> list) {
        NavigableMap<Integer, List<Log<T>>> writes = new TreeMap<>();
        for (Log log : list) {
            if (log.type != LogType.WRITE)
                continue;
            for (int time = log.before; time <= log.after; ++time) {
                List<Log<T>> onTime = writes.get(time);
                if (onTime == null) {
                    writes.put(time, onTime = new ArrayList<>());
                }
                onTime.add(log);
            }
        }
        return writes;
    }

    private <T> void checkCorrectness(String dumpPrefix, List<Log<T>> logs, NavigableMap<Integer, List<Log<T>>> writesByTime) {
        Collections.sort(logs, WALL_CLOCK_TIME_COMPARATOR);
        int nullReads = 0, reads = 0, writes = 0;
        for (Log read : logs) {
            if (read.type != LogType.READ) {
                writes++;
                continue;
            }
            if (read.getValue() == null || isEmptyCollection(read))
                nullReads++;
            else
                reads++;

            Map<T, Log<T>> possibleValues = new HashMap<>();
            for (List<Log<T>> list : writesByTime.subMap(read.before, true, read.after, true).values()) {
                for (Log<T> write : list) {
                    if (read.precedes(write))
                        continue;
                    possibleValues.put(write.getValue(), write);
                }
            }
            int startOfLastWriteBeforeRead = 0;
            for (Map.Entry<Integer, List<Log<T>>> entry : writesByTime.headMap(read.before, false).descendingMap().entrySet()) {
                int time = entry.getKey();
                if (time < startOfLastWriteBeforeRead)
                    break;
                for (Log<T> write : entry.getValue()) {
                    if (write.after < read.before && write.before > startOfLastWriteBeforeRead) {
                        startOfLastWriteBeforeRead = write.before;
                    }
                    possibleValues.put(write.getValue(), write);
                }
            }

            if (possibleValues.isEmpty()) {
                // the entry was not created at all (first write failed)
                break;
            }
            if (!possibleValues.containsKey(read.getValue())) {
                dumpLogs(dumpPrefix, logs);
                exceptions.add(
                        new IllegalStateException(String.format("R %s: %d .. %d (%s, %s) -> %s not in %s (%d+)", dumpPrefix,
                                read.before, read.after, read.threadName,
                                new SimpleDateFormat("HH:mm:ss,SSS").format(new Date(read.wallClockTime)),
                                read.getValue(), possibleValues.values(), startOfLastWriteBeforeRead)));
                break;
            }
        }
        log.infof("Checked %d null reads, %d reads and %d writes%n", nullReads, reads, writes);
    }

    private static boolean isEmptyCollection(Log read) {
        return read.getValue() instanceof java.util.Collection && ((java.util.Collection) read.getValue()).isEmpty();
    }

    private <T> void dumpLogs(String prefix, List<Log<T>> logs) {
        try {
            File f = File.createTempFile(prefix, ".log");
            log.info("Dumping logs into " + f.getAbsolutePath());
            try (BufferedWriter writer = Files.newBufferedWriter(f.toPath())) {
                for (Log<T> log : logs) {
                    writer.write(log.toString());
                    writer.write('\n');
                }
            }
        } catch (IOException e) {
            log.error("Failed to dump family logs");
        }
    }

    private Operation getOperation() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Operation operation;
        int r = random.nextInt(100);
        if (r == 0 && INVALIDATE_REGION)
            operation = new InvalidateCache();
        else if (r < 5)
            operation = new QueryFamilies();
        else if (r < 10)
            operation = new RemoveFamily(r < 6);
        else if (r < 20)
            operation = new UpdateFamily(r < 12, random.nextInt(1, 3));
        else if (r < 35)
            operation = new AddMember(r < 25);
        else if (r < 50)
            operation = new RemoveMember(r < 40);
        else
            operation = new ReadFamily(r < 75);
        return operation;
    }

    private class ReadFamily extends Operation {
        private final boolean evict;

        public ReadFamily(boolean evict) {
            super(false);
            this.evict = evict;
        }

        @Override
        public void run() throws Exception {
            withRandomFamily((s, f) -> {
                if (evict) {
                    sessionFactory.getCache().evictEntity(familyClass, f.getId());
                }
            }, Ref.empty(), Ref.empty(), null);
        }
    }

    private class RemoveMember extends MemberOperation {
        public RemoveMember(boolean rolledBack) {
            super(rolledBack);
        }

        @Override
        protected boolean updateMembers(Session s, ThreadLocalRandom random, Family f) {
            int numMembers = f.getMembers().size();
            if (numMembers > 0) {
                Iterator<? extends Member> it = f.getMembers().iterator();
                Member member = null;
                for (int i = random.nextInt(numMembers); i >= 0; --i) {
                    member = it.next();
                }
                it.remove();
                if (member != null) {
                    s.delete(member);
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private class InvalidateCache extends Operation {
        public InvalidateCache() {
            super(false);
        }

        @Override
        public void run() throws Exception {
            log.trace("Invalidating all caches");
            sessionFactory.getCache().evictAllRegions();
        }
    }

    private abstract class MemberOperation extends Operation {
        public MemberOperation(boolean rolledBack) {
            super(rolledBack);
        }

        @Override
        public void run() throws Exception {
            Ref<Set<String>> newMembers = new Ref<>();
            withRandomFamily((s, f) -> {
                boolean updated = updateMembers(s, ThreadLocalRandom.current(), f);
                if (updated) {
                    newMembers.set(membersToNames(f.getMembers()));
                    s.persist(f);
                }
            }, Ref.empty(), newMembers, LockMode.OPTIMISTIC_FORCE_INCREMENT);
        }

        protected abstract boolean updateMembers(Session s, ThreadLocalRandom random, Family f);
    }

    private class AddMember extends MemberOperation {
        public AddMember(boolean rolledBack) {
            super(rolledBack);
        }

        protected boolean updateMembers(Session s, ThreadLocalRandom random, Family f) {
            Set<? extends Member> members = f.getMembers();
            if (members.size() < MAX_MEMBERS) {
                members.add(createMember(f));
                return true;
            } else {
                return false;
            }
        }

        private <T extends Member> T createMember(Family f) {
            return (T) memberCtor.apply(f);
        }
    }

    private class UpdateFamily extends Operation {
        private final int numUpdates;

        public UpdateFamily(boolean rolledBack, int numUpdates) {
            super(rolledBack);
            this.numUpdates = numUpdates;
        }

        @Override
        public void run() throws Exception {
            String[] newNames = new String[numUpdates];
            for (int i = 0; i < numUpdates; ++i) {
                newNames[i] = randomString(ThreadLocalRandom.current());
            }
            withRandomFamilies(numUpdates, (s, families) -> {
                for (int i = 0; i < numUpdates; ++i) {
                    Family f = families[i];
                    if (f != null) {
                        f.setName(newNames[i]);
                        s.persist(f);
                    }
                }
            }, newNames, null, LockMode.OPTIMISTIC_FORCE_INCREMENT);
        }
    }

    private class RemoveFamily extends Operation {
        public RemoveFamily(boolean rolledBack) {
            super(rolledBack);
        }

        @Override
        public void run() throws Exception {
            withRandomFamily((s, f) -> s.delete(f), Ref.of(null), Ref.of(Collections.EMPTY_SET), LockMode.OPTIMISTIC);
        }
    }

    private class QueryFamilies extends Operation {
        final static int MAX_RESULTS = 10;

        public QueryFamilies() {
            super(false);
        }

        @Override
        public void run() throws Exception {
            String prefix = new StringBuilder(2)
                    .append((char) ThreadLocalRandom.current().nextInt('A', 'Z' + 1)).append('%').toString();
            int[] ids = new int[MAX_RESULTS];
            String[] names = new String[MAX_RESULTS];
            Set<String>[] members = new Set[MAX_RESULTS];

            int before = timestampGenerator.getAndIncrement();
            log.tracef("Started QueryFamilies at %d", before);
            withSession(s -> {
                List<Family> results = s.createCriteria(familyClass)
                        .add(Restrictions.like("name", prefix))
                        .setMaxResults(MAX_RESULTS)
                        .setCacheable(true)
                        .list();
                int index = 0;
                for (Family f : results) {
                    ids[index] = f.getId();
                    names[index] = f.getName();
                    members[index] = membersToNames(f.getMembers());
                    ++index;
                }
            });

            int after = timestampGenerator.getAndIncrement();
            log.tracef("Finished QueryFamilies at %d", after);
            for (int index = 0; index < MAX_RESULTS; ++index) {
                if (names[index] == null)
                    break;
                getRecordList(familyNames, ids[index]).add(new Log<>(before, after, names[index], LogType.READ));
                getRecordList(familyMembers, ids[index]).add(new Log<>(before, after, members[index], LogType.READ));
            }
        }
    }

    private abstract class Operation {
        protected final boolean rolledBack;

        public Operation(boolean rolledBack) {
            this.rolledBack = rolledBack;
        }

        public abstract void run() throws Exception;

        protected void withSession(Consumer<Session> consumer) throws Exception {
            Session s = sessionFactory.openSession();
            Transaction tx = s.getTransaction();
            tx.begin();
            try {
                consumer.accept(s);
            } catch (Exception e) {
                tx.markRollbackOnly();
                throw e;
            } finally {
                try {
                    if (!rolledBack && tx.getStatus() == TransactionStatus.ACTIVE) {
                        log.trace("Hibernate commit begin");
                        tx.commit();
                        log.trace("Hibernate commit end");
                    } else {
                        log.trace("Hibernate rollback begin");
                        tx.rollback();
                        log.trace("Hibernate rollback end");
                    }
                } catch (Exception e) {
                    log.trace("Hibernate commit or rollback failed, status is " + tx.getStatus(), e);
                    if (tx.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
                        tx.rollback();
                    }
                    throw e;
                } finally {
                    // cannot close before XA commit since force increment requires open connection
                    s.close();
                }
            }
        }

        protected void withRandomFamily(BiConsumer<Session, Family> consumer, Ref<String> familyNameUpdate,
                Ref<Set<String>> familyMembersUpdate, LockMode lockMode) throws Exception {
            int id = randomFamilyId(ThreadLocalRandom.current());
            int before = timestampGenerator.getAndIncrement();
            log.tracef("Started %s(%d, %s) at %d", getClass().getSimpleName(), id, rolledBack, before);
            Log<String> familyNameLog = new Log<>();
            Log<Set<String>> familyMembersLog = new Log<>();

            boolean failure = false;
            try {
                withSession(s -> {
                    Family f = lockMode != null ? s.get(familyClass, id, lockMode) : s.get(familyClass, id);
                    if (f == null) {
                        familyNameLog.setValue(null);
                        familyMembersLog.setValue(Collections.EMPTY_SET);
                        familyNotFound(id);
                    } else {
                        familyNameLog.setValue(f.getName());
                        familyMembersLog.setValue(membersToNames(f.getMembers()));
                        consumer.accept(s, f);
                    }
                });
            } catch (Exception e) {
                failure = true;
                throw e;
            } finally {
                int after = timestampGenerator.getAndIncrement();
                recordReadWrite(id, before, after, failure, familyNameUpdate, familyMembersUpdate, familyNameLog,
                        familyMembersLog);
            }
        }

        protected void withRandomFamilies(int numFamilies, BiConsumer<Session, Family[]> consumer, String[] familyNameUpdates,
                Set<String>[] familyMembersUpdates, LockMode lockMode) throws Exception {
            int ids[] = new int[numFamilies];
            Log<String>[] familyNameLogs = new Log[numFamilies];
            Log<Set<String>>[] familyMembersLogs = new Log[numFamilies];
            for (int i = 0; i < numFamilies; ++i) {
                ids[i] = randomFamilyId(ThreadLocalRandom.current());
                familyNameLogs[i] = new Log<>();
                familyMembersLogs[i] = new Log<>();
            }
            int before = timestampGenerator.getAndIncrement();
            log.tracef("Started %s(%s) at %d", getClass().getSimpleName(), Arrays.toString(ids), before);

            boolean failure = false;
            try {
                withSession(s -> {
                    Family[] families = new Family[numFamilies];
                    for (int i = 0; i < numFamilies; ++i) {
                        Family f = lockMode != null ? s.get(familyClass, ids[i], lockMode) : s.get(familyClass, ids[i]);
                        families[i] = f;
                        if (f == null) {
                            familyNameLogs[i].setValue(null);
                            familyMembersLogs[i].setValue(Collections.EMPTY_SET);
                            familyNotFound(ids[i]);
                        } else {
                            familyNameLogs[i].setValue(f.getName());
                            familyMembersLogs[i].setValue(membersToNames(f.getMembers()));
                        }
                    }
                    consumer.accept(s, families);
                });
            } catch (Exception e) {
                failure = true;
                throw e;
            } finally {
                int after = timestampGenerator.getAndIncrement();
                for (int i = 0; i < numFamilies; ++i) {
                    recordReadWrite(ids[i], before, after, failure,
                            familyNameUpdates != null ? Ref.of(familyNameUpdates[i]) : Ref.empty(),
                            familyMembersUpdates != null ? Ref.of(familyMembersUpdates[i]) : Ref.empty(),
                            familyNameLogs[i], familyMembersLogs[i]);
                }
            }
        }

        private void recordReadWrite(int id, int before, int after, boolean failure, Ref<String> familyNameUpdate,
                Ref<Set<String>> familyMembersUpdate, Log<String> familyNameLog, Log<Set<String>> familyMembersLog) {
            log.tracef("Finished %s at %d", getClass().getSimpleName(), after);

            LogType readType, writeType;
            if (failure || rolledBack) {
                writeType = LogType.WRITE_FAILURE;
                readType = LogType.READ_FAILURE;
            } else {
                writeType = LogType.WRITE;
                readType = LogType.READ;
            }

            familyNameLog.setType(readType).setTimes(before, after);
            familyMembersLog.setType(readType).setTimes(before, after);

            getRecordList(familyNames, id).add(familyNameLog);
            getRecordList(familyMembers, id).add(familyMembersLog);

            if (familyNameLog.getValue() != null) {
                if (familyNameUpdate.isSet()) {
                    getRecordList(familyNames, id)
                            .add(new Log<>(before, after, familyNameUpdate.get(), writeType, familyNameLog));
                }
                if (familyMembersUpdate.isSet()) {
                    getRecordList(familyMembers, id)
                            .add(new Log<>(before, after, familyMembersUpdate.get(), writeType, familyMembersLog));
                }
            }
        }
    }

    private class InsertFamily extends Operation {
        public InsertFamily(boolean rolledBack) {
            super(rolledBack);
        }

        @Override
        public void run() throws Exception {
            Family family = familyCtor.get();
            int before = timestampGenerator.getAndIncrement();
            log.trace("Started InsertFamily at " + before);
            boolean failure = false;
            try {
                withSession(s -> s.persist(family));
            } catch (Exception e) {
                failure = true;
                throw e;
            } finally {
                int after = timestampGenerator.getAndIncrement();
                log.trace("Finished InsertFamily at " + after + ", " + (failure ? "failed" : "success"));
                familyIds.put(family.getId(), new AtomicInteger(NUM_ACCESS_AFTER_REMOVAL));
                LogType type = failure || rolledBack ? LogType.WRITE_FAILURE : LogType.WRITE;
                getRecordList(familyNames, family.getId()).add(new Log<>(before, after, family.getName(), type));
                getRecordList(familyMembers, family.getId())
                        .add(new Log<>(before, after, membersToNames(family.getMembers()), type));
            }
        }
    }

    private void familyNotFound(int id) {
        AtomicInteger access = familyIds.get(id);
        if (access == null)
            return;
        if (access.decrementAndGet() == 0) {
            familyIds.remove(id);
        }
    }

    private static Set<String> membersToNames(Set<? extends Member> members) {
        return members.stream().map(p -> p.getFirstName()).collect(Collectors.toSet());
    }

    private static <T> List<T> getRecordList(ThreadLocal<Map<Integer, List<T>>> tlListMap, int id) {
        Map<Integer, List<T>> map = tlListMap.get();
        List<T> list = map.get(id);
        if (list == null)
            map.put(id, list = new ArrayList<>());
        return list;
    }

    private int randomFamilyId(ThreadLocalRandom random) {
        Map.Entry<Integer, AtomicInteger> first = familyIds.firstEntry();
        Map.Entry<Integer, AtomicInteger> last = familyIds.lastEntry();
        if (first == null || last == null)
            return 0;
        Map.Entry<Integer, AtomicInteger> ceiling = familyIds.ceilingEntry(random.nextInt(first.getKey(), last.getKey() + 1));
        return ceiling == null ? 0 : ceiling.getKey();
    }

    public static String randomString(ThreadLocalRandom random) {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; ++i) {
            sb.append((char) random.nextInt('A', 'Z' + 1));
        }
        return sb.toString();
    }

    private enum LogType {
        READ('R'), WRITE('W'), READ_FAILURE('L'), WRITE_FAILURE('F');

        private final char shortName;

        LogType(char shortName) {
            this.shortName = shortName;
        }
    }

    private static final class Log<T> {
        int before;
        int after;
        T value;
        LogType type;
        Log[] preceding;
        String threadName;
        long wallClockTime;

        public Log(int time) {
            this();
            this.before = time;
            this.after = time;
        }

        public Log(int before, int after, T value, LogType type, Log<T>... preceding) {
            this();
            this.before = before;
            this.after = after;
            this.value = value;
            this.type = type;
            this.preceding = preceding;
        }

        public Log() {
            threadName = Thread.currentThread().getName();
            wallClockTime = System.currentTimeMillis();
        }

        public Log setType(LogType type) {
            this.type = type;
            return this;
        }

        public void setTimes(int before, int after) {
            this.before = before;
            this.after = after;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public boolean precedes(Log<T> write) {
            if (write.preceding == null)
                return false;
            for (Log<T> l : write.preceding) {
                if (l == this)
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("%c: %5d - %5d\t(%s,\t%s)\t%s", type.shortName, before, after,
                    new SimpleDateFormat("HH:mm:ss,SSS").format(new Date(wallClockTime)), threadName, value);
        }
    }

    private static class Ref<T> {
        private static Ref EMPTY = new Ref() {
            @Override
            public void set(Object value) {
                throw new UnsupportedOperationException();
            }
        };
        private boolean set;
        private T value;

        public static <T> Ref<T> empty() {
            return EMPTY;
        }

        public static <T> Ref<T> of(T value) {
            Ref ref = new Ref();
            ref.set(value);
            return ref;
        }

        public boolean isSet() {
            return set;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
            this.set = true;
        }
    }

    public static class InducedException extends Exception {
        public InducedException(String message) {
            super(message);
        }
    }

}
