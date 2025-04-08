CREATE TABLE IF NOT EXISTS cod_entry (
    id bigserial PRIMARY KEY,
    cod_id varchar(255) UNIQUE,
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
