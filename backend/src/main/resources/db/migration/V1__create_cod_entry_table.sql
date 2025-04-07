create table cod_entry (
    id bigserial primary key,
    cod_id varchar(255) unique,
    mineral_name varchar(256),
    formula varchar(257),
    elements varchar(258),
    publication_year varchar(259),
    authors varchar(260),
    journal varchar(261),
    doi varchar(262),
    download_url varchar(263),
    last_updated timestamp
);
