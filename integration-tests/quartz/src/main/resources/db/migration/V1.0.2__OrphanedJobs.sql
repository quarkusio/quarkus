-- A job left in the job store by a scheduled method that no longer exists, removed with quarkus.quartz.remove-orphaned-jobs
INSERT INTO QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_NONCONCURRENT, IS_UPDATE_DATA, REQUESTS_RECOVERY, JOB_DATA)
VALUES ('QuarkusQuartzScheduler', 'removed-method', 'io.quarkus.scheduler.Scheduler', NULL, 'io.quarkus.quartz.runtime.QuartzSchedulerImpl$InvokerJob', FALSE, FALSE, FALSE, TRUE, NULL);

INSERT INTO QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, JOB_NAME, JOB_GROUP, DESCRIPTION, NEXT_FIRE_TIME, PREV_FIRE_TIME, PRIORITY, TRIGGER_STATE, TRIGGER_TYPE, START_TIME, END_TIME, CALENDAR_NAME, MISFIRE_INSTR, JOB_DATA)
VALUES ('QuarkusQuartzScheduler', 'removed-method', 'io.quarkus.scheduler.Scheduler', 'removed-method', 'io.quarkus.scheduler.Scheduler', NULL, 4102444800000, NULL, 5, 'WAITING', 'SIMPLE', 4102444800000, 0, NULL, 0, NULL);

INSERT INTO QRTZ_SIMPLE_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, REPEAT_COUNT, REPEAT_INTERVAL, TIMES_TRIGGERED)
VALUES ('QuarkusQuartzScheduler', 'removed-method', 'io.quarkus.scheduler.Scheduler', -1, 60000, 0);

-- A job that was not created by Quarkus for a scheduled method, which must be kept
INSERT INTO QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, IS_NONCONCURRENT, IS_UPDATE_DATA, REQUESTS_RECOVERY, JOB_DATA)
VALUES ('QuarkusQuartzScheduler', 'quartz-api-job', 'io.quarkus.scheduler.Scheduler', NULL, 'io.quarkus.it.quartz.ManualScheduledCounter$CountingJob', TRUE, FALSE, FALSE, FALSE, NULL);
