--liquibase formatted sql-- liquibase formatted sql

-- changeset pawellagan:hibernate-sequence
CREATE SEQUENCE  IF NOT EXISTS "public"."hibernate_sequence" AS bigint START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pawellagan:persistent-events
CREATE TABLE "public"."persistent_event" ("id" BIGINT NOT NULL, "created_at" TIMESTAMP WITHOUT TIME ZONE, "entity_id" BIGINT, "entity_name" VARCHAR(255), "event_data" VARCHAR(255), "event_name" VARCHAR(255), "parent_id" VARCHAR(255), "span_id" VARCHAR(255), "trace_id" VARCHAR(255), "version" BIGINT NOT NULL, CONSTRAINT "persistent_event_pkey" PRIMARY KEY ("id"));

CREATE TABLE IF NOT EXISTS public.persistent_event_version
(
    entity_id bigint NOT NULL,
    entity_name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    "timestamp" timestamp without time zone,
    version bigint,
    CONSTRAINT persistent_event_version_pkey PRIMARY KEY (entity_id, entity_name)
)