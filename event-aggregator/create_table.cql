CREATE TABLE entrypoint_by_day (
    id text,
    date text,
    event_timestamp timestamp,
    event_name text,
    correlation_id uuid,
    haproxy_id text,
    payload text,
    PRIMARY KEY ((id,date),event_timestamp)
) WITH CLUSTERING ORDER BY (event_timestamp DESC);
