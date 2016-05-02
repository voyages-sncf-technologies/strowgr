CREATE TABLE entrypoint_by_day (
    id text,
    date text,
    event_timestamp timestamp,
    correlation_id uuid,
    event_name text,
    haproxy_id text,
    payload text,
    PRIMARY KEY ((id,date),event_timestamp, correlation_id, event_name)
) WITH CLUSTERING ORDER BY (event_timestamp DESC);

CREATE TABLE error_by_day (
    date text,
    timestamp timestamp,
    reason text,
    payload text,
    PRIMARY KEY (date, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);
