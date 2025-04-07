create table cod_query (
    id bigserial primary key,
    element_set varchar(512),
    requested_at timestamp,
    completed boolean
);
