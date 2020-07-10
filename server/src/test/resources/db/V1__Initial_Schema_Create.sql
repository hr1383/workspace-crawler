CREATE TABLE IF NOT EXISTS accounts (
    account_sid varchar NOT NULL,
    account_name varchar NOT NULL,
    time_created bigint,
    time_updated bigint,
    is_active boolean
);

CREATE TABLE IF EXISTS roles (
    role_sid varchar NOT NULL,
    role_name varchar NOT NULL
);

CREATE TABLE IF EXISTS connector (
    account_sid varchar NOT NULL,
    connector_sid varchar NOT NULL,
    time_created bigint,
    time_updated bigint,
    time_last_sync bigint
);

CREATE TABLE IF EXISTS connector_details (
    account_sid varchar NOT NULL,
    connector_sid varchar NOT NULL,
    connector_detail_sid varchar NOT NULL,
    connector_type varchar NOT NULL,
    connector_credentials varchar NOT NULL,
    time_created bigint,
    time_updated bigint,
    time_last_sync bigint
);

CREATE TABLE IF NOT EXISTS username (
   user_sid varchar NOT NULL,
   user_name varchar NOT NULL,
   account_sid varchar NOT NULL,
   time_created bigint,
   time_last_login bigint,
   is_active boolean NOT NULL
);

CREATE TABLE IF NOT EXISTS user_preference (
   user_sid integer NOT NULL,
   user_pref_sid varchar NOT NULL,
   user_pref_name varchar NOT NULL,
   user_pref_loc varchar NOT NULL,
   time_created bigint,
   time_updated bigint
);