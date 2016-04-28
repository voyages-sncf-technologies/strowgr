CREATE TABLE entrypoint_by_day (
    id text,
    date text,
    event_timestamp timestamp,
    correlation_id uuid,
    event_name text,
    haproxy_id text,
    payload text,
    PRIMARY KEY ((id,date),event_timestamp,correlation_id)
) WITH CLUSTERING ORDER BY (event_timestamp DESC);
