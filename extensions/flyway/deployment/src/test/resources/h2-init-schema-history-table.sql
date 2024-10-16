CREATE TABLE "flyway_schema_history" (
    "installed_rank" integer NOT NULL,
    "version" character varying(50),
    "description" character varying(200) NOT NULL,
    "type" character varying(20) NOT NULL,
    "script" character varying(1000) NOT NULL,
    "checksum" integer,
    "installed_by" character varying(100) NOT NULL,
    "installed_on" timestamp without time zone DEFAULT now() NOT NULL,
    "execution_time" integer NOT NULL,
    "success" boolean NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY ("installed_rank")
);