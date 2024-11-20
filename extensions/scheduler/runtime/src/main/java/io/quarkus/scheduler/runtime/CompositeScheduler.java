package io.quarkus.scheduler.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;

import io.quarkus.arc.All;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.quarkus.scheduler.common.runtime.AbstractJobDefinition;
import io.quarkus.scheduler.common.runtime.SchedulerContext;

/**
 * The composite scheduler is only used in case of multiple {@link Scheduler} implementations are required.
 *
 * @see Scheduled#executeWith()
 */
@Typed(Scheduler.class)
@Singleton
public class CompositeScheduler implements Scheduler {

    private final List<Scheduler> schedulers;

    private final SchedulerContext schedulerContext;

    CompositeScheduler(@All @Constituent List<Scheduler> schedulers, SchedulerContext schedulerContext) {
        this.schedulers = schedulers;
        this.schedulerContext = schedulerContext;
    }

    @Override
    public boolean isStarted() {
        // IMPL NOTE: we return true if at least one of the schedulers is started
        for (Scheduler scheduler : schedulers) {
            if (scheduler.isStarted()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void pause() {
        for (Scheduler scheduler : schedulers) {
            scheduler.pause();
        }
    }

    @Override
    public void pause(String identity) {
        for (Scheduler scheduler : schedulers) {
            scheduler.pause(identity);
        }
    }

    @Override
    public void resume() {
        for (Scheduler scheduler : schedulers) {
            scheduler.resume();
        }
    }

    @Override
    public void resume(String identity) {
        for (Scheduler scheduler : schedulers) {
            scheduler.resume(identity);
        }
    }

    @Override
    public boolean isPaused(String identity) {
        for (Scheduler scheduler : schedulers) {
            if (scheduler.isPaused(identity)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRunning() {
        // IMPL NOTE: we return true if at least one of the schedulers is running
        for (Scheduler scheduler : schedulers) {
            if (scheduler.isRunning()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Trigger> getScheduledJobs() {
        List<Trigger> triggers = new ArrayList<>();
        for (Scheduler scheduler : schedulers) {
            triggers.addAll(scheduler.getScheduledJobs());
        }
        return triggers;
    }

    @Override
    public Trigger getScheduledJob(String identity) {
        for (Scheduler scheduler : schedulers) {
            Trigger trigger = scheduler.getScheduledJob(identity);
            if (trigger != null) {
                return trigger;
            }
        }
        return null;
    }

    @Override
    public CompositeJobDefinition newJob(String identity) {
        return new CompositeJobDefinition(identity);
    }

    @Override
    public Trigger unscheduleJob(String identity) {
        for (Scheduler scheduler : schedulers) {
            Trigger trigger = scheduler.unscheduleJob(identity);
            if (trigger != null) {
                return trigger;
            }
        }
        return null;
    }

    @Override
    public String implementation() {
        return Scheduled.AUTO;
    }

    public class CompositeJobDefinition extends AbstractJobDefinition<CompositeJobDefinition> {

        public CompositeJobDefinition(String identity) {
            super(identity);
        }

        @Override
        public CompositeJobDefinition setExecuteWith(String implementation) {
            Objects.requireNonNull(implementation);
            if (!Scheduled.AUTO.equals(implementation)) {
                if (schedulers.stream().map(Scheduler::implementation).noneMatch(implementation::equals)) {
                    throw new IllegalArgumentException("Scheduler implementation not available: " + implementation);
                }
            }
            return super.setExecuteWith(implementation);
        }

        @Override
        public Trigger schedule() {
            String impl = implementation;
            if (Scheduled.AUTO.equals(impl)) {
                impl = schedulerContext.autoImplementation();
            }
            for (Scheduler scheduler : schedulers) {
                if (scheduler.implementation().equals(impl)) {
                    return copy(scheduler.newJob(identity)).schedule();
                }
            }
            throw new IllegalStateException("Matching scheduler implementation not found: " + implementation);
        }

        private JobDefinition<?> copy(JobDefinition<?> to) {
            to.setCron(cron);
            to.setInterval(every);
            to.setDelayed(delayed);
            to.setOverdueGracePeriod(overdueGracePeriod);
            to.setConcurrentExecution(concurrentExecution);
            to.setTimeZone(timeZone);
            to.setExecuteWith(implementation);
            if (skipPredicateClass != null) {
                to.setSkipPredicate(skipPredicateClass);
            } else if (skipPredicate != null) {
                to.setSkipPredicate(skipPredicate);
            }
            if (taskClass != null) {
                if (runOnVirtualThread) {
                    to.setTask(taskClass, runOnVirtualThread);
                } else {
                    to.setTask(taskClass);
                }
            } else if (task != null) {
                if (runOnVirtualThread) {
                    to.setTask(task, runOnVirtualThread);
                } else {
                    to.setTask(task);
                }
            }
            if (asyncTaskClass != null) {
                to.setAsyncTask(asyncTaskClass);
            } else if (asyncTask != null) {
                to.setAsyncTask(asyncTask);
            }
            return to;
        }

    }

}
