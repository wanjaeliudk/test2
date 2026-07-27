--liquibase formatted sql

--changeset copilot:001-create-ussd-session-schema
create table if not exists ussd_session (
    id uuid primary key,
    date_created timestamp not null default current_timestamp,
    date_modified timestamp not null default current_timestamp,
    created_by varchar(255),
    updated_by varchar(255),
    session_id varchar(100) not null unique,
    msisdn varchar(20) not null,
    flow_code varchar(100) not null,
    current_node_key varchar(100) not null,
    status varchar(20) not null,
    history_json text,
    variables_json text
);

create index if not exists idx_ussd_session_status on ussd_session (status);

--rollback drop table if exists ussd_session;
