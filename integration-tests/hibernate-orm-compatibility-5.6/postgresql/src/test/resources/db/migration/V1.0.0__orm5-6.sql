--
-- PostgreSQL database dump
--

-- Dumped from database version 14.7 (Debian 14.7-1.pgdg110+1)
-- Dumped by pg_dump version 14.7 (Debian 14.7-1.pgdg110+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: gengendefallocsize; Type: SEQUENCE; Schema: public; Owner: hibernate_orm_test
--

CREATE SEQUENCE public.gengendefallocsize
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.gengendefallocsize OWNER TO hibernate_orm_test;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: hibernate_orm_test
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO hibernate_orm_test;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: myentity; Type: TABLE; Schema: public; Owner: hibernate_orm_test
--

CREATE TABLE public.myentity (
    id bigint NOT NULL,
    duration bigint,
    instant timestamp without time zone,
    intarray bytea,
    myenum integer,
    offsetdatetime timestamp without time zone,
    offsettime time without time zone,
    stringlist bytea,
    uuid uuid,
    zoneddatetime timestamp without time zone
);


ALTER TABLE public.myentity OWNER TO hibernate_orm_test;

--
-- Name: myentity_gengendefallocsize; Type: TABLE; Schema: public; Owner: hibernate_orm_test
--

CREATE TABLE public.myentity_gengendefallocsize (
    id bigint NOT NULL
);


ALTER TABLE public.myentity_gengendefallocsize OWNER TO hibernate_orm_test;

--
-- Name: myentity_seqgendefallocsize; Type: TABLE; Schema: public; Owner: hibernate_orm_test
--

CREATE TABLE public.myentity_seqgendefallocsize (
    id bigint NOT NULL
);


ALTER TABLE public.myentity_seqgendefallocsize OWNER TO hibernate_orm_test;

--
-- Name: seqgendefallocsize; Type: SEQUENCE; Schema: public; Owner: hibernate_orm_test
--

CREATE SEQUENCE public.seqgendefallocsize
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seqgendefallocsize OWNER TO hibernate_orm_test;

--
-- Data for Name: myentity; Type: TABLE DATA; Schema: public; Owner: hibernate_orm_test
--

COPY public.myentity (id, duration, instant, intarray, myenum, offsetdatetime, offsettime, stringlist, uuid, zoneddatetime) FROM stdin;
1	59000000000	2018-01-01 11:58:30	\\xaced0005757200025b494dba602676eab2a502000078700000000300000000000000010000002a	1	2018-01-01 11:58:30	12:58:30	\\xaced0005737200136a6176612e7574696c2e41727261794c6973747881d21d99c7619d03000149000473697a657870000000027704000000027400036f6e6574000374776f78	f49c6ba8-8d7f-417a-a255-d594dddf729f	2018-01-01 11:58:30
2	\N	\N	\N	\N	\N	\N	\N	\N	\N
\.


--
-- Data for Name: myentity_gengendefallocsize; Type: TABLE DATA; Schema: public; Owner: hibernate_orm_test
--

COPY public.myentity_gengendefallocsize (id) FROM stdin;
1
2
\.


--
-- Data for Name: myentity_seqgendefallocsize; Type: TABLE DATA; Schema: public; Owner: hibernate_orm_test
--

COPY public.myentity_seqgendefallocsize (id) FROM stdin;
1
2
\.


--
-- Name: gengendefallocsize; Type: SEQUENCE SET; Schema: public; Owner: hibernate_orm_test
--

SELECT pg_catalog.setval('public.gengendefallocsize', 2, true);


--
-- Name: hibernate_sequence; Type: SEQUENCE SET; Schema: public; Owner: hibernate_orm_test
--

SELECT pg_catalog.setval('public.hibernate_sequence', 2, true);


--
-- Name: seqgendefallocsize; Type: SEQUENCE SET; Schema: public; Owner: hibernate_orm_test
--

SELECT pg_catalog.setval('public.seqgendefallocsize', 51, true);


--
-- Name: myentity_gengendefallocsize myentity_gengendefallocsize_pkey; Type: CONSTRAINT; Schema: public; Owner: hibernate_orm_test
--

ALTER TABLE ONLY public.myentity_gengendefallocsize
    ADD CONSTRAINT myentity_gengendefallocsize_pkey PRIMARY KEY (id);


--
-- Name: myentity myentity_pkey; Type: CONSTRAINT; Schema: public; Owner: hibernate_orm_test
--

ALTER TABLE ONLY public.myentity
    ADD CONSTRAINT myentity_pkey PRIMARY KEY (id);


--
-- Name: myentity_seqgendefallocsize myentity_seqgendefallocsize_pkey; Type: CONSTRAINT; Schema: public; Owner: hibernate_orm_test
--

ALTER TABLE ONLY public.myentity_seqgendefallocsize
    ADD CONSTRAINT myentity_seqgendefallocsize_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

