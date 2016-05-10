package com.vsct.haas.monitoring.gui.cassandra;

import com.datastax.driver.core.*;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class CassandraClient {

    static class Value {
        Date   timestamp;
        String name;
        String payload;
    }

    static class Key {
        String correlationId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (correlationId != null ? !correlationId.equals(key.correlationId) : key.correlationId != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return correlationId != null ? correlationId.hashCode() : 0;
        }
    }

    static LinkedHashMultimap<Key, Value> result = LinkedHashMultimap.create();

    static AtomicInteger messageProcessed = new AtomicInteger(0);
    static Boolean showPayload;

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        String node = System.getenv("cassandra.node");
        String keyspace = System.getenv("cassandra.keyspace");
        String entrypoint = System.getenv("entrypoint");
        String date = System.getenv("date");
        String since = System.getenv("since");
        showPayload = Boolean.valueOf(System.getenv("showPayload"));

        Date sinceDate = null;
        if (since != null && since.contains("s"))
            sinceDate = Date.from(LocalDateTime.now().minusSeconds(Long.valueOf(since.replace("s", ""))).atZone(ZoneId.systemDefault()).toInstant());
        if (since != null && since.contains("m"))
            sinceDate = Date.from(LocalDateTime.now().minusMinutes(Long.valueOf(since.replace("m", ""))).atZone(ZoneId.systemDefault()).toInstant());
        if (since != null && since.contains("h"))
            sinceDate = Date.from(LocalDateTime.now().minusHours(Long.valueOf(since.replace("h", ""))).atZone(ZoneId.systemDefault()).toInstant());

        if (node == null | keyspace == null | entrypoint == null | date == null | since == null | showPayload == null | sinceDate == null) {
            System.out.println("You must provide these environment variables :");
            System.out.println("\t- cassandra.node");
            System.out.println("\t- cassandra.keyspace");
            System.out.println("\t- entrypoint (ex. PAO/REL1)");
            System.out.println("\t- date (ex. 2016-04-28)");
            System.out.println("\t- since (ex. 10s|25m|2h)");
            System.out.println("\t- showPayload (true or false)");
            return;
        }

        System.out.println("Initiating connection with Cassandra on "+node);
        
        Cluster cluster = Cluster.builder()
                .addContactPoint(node)
                .withQueryOptions(new QueryOptions().setFetchSize(500))
                .build();

        Session session = cluster.connect(keyspace);

        String query;
        if (showPayload) {
            query = "select event_timestamp,event_name,correlation_id,payload from entrypoint_by_day where id='" + entrypoint + "' and date='" + date + "' and event_timestamp > " + sinceDate.getTime() + ";";
        }
        else {
            query = "select event_timestamp,event_name,correlation_id from entrypoint_by_day where id='" + entrypoint + "' and date='" + date + "' and event_timestamp > " + sinceDate.getTime() + ";";
        }

        System.out.println("Cassandra query=" + query+"\n");

        Statement statement = new SimpleStatement(query);

        ResultSetFuture resultSetFuture = session.executeAsync(statement);
        ListenableFuture<Multimap<Key, Value>> futur = Futures.transform(
                resultSetFuture,
                iterate(result)
        );

        futur.get().keySet().forEach(k -> {
            System.out.print(k.correlationId + " END");
            result.get(k).forEach(v -> {
                if (showPayload) {
                    System.out.print(" <- "+v.name +"("+v.timestamp+")|" + v.payload + "|");
                }
                else {
                    System.out.print(" <- "+v.name +"("+v.timestamp+")");
                }
            });
            System.out.println(" <- BEGIN");
        });

        System.out.println("\nClosing Cassandra connection");
        session.close();
        cluster.close();
    }

    private static AsyncFunction<ResultSet, Multimap<Key, Value>> iterate(Multimap<Key, Value> result) {
        return new AsyncFunction<ResultSet, Multimap<Key, Value>>() {
            @Override
            public ListenableFuture<Multimap<Key, Value>> apply(ResultSet rs) throws Exception {
                int remaining = rs.getAvailableWithoutFetching();

                for (Row row : rs) {
                    Date d = row.get(0, Date.class);
                    String name = row.get(1, String.class);
                    String correlationId = row.get(2, UUID.class).toString();

                    Key k = new Key();
                    k.correlationId = correlationId;
                    Value v = new Value();
                    v.name = name;
                    v.timestamp = d;

                    if (showPayload) {
                        v.payload = row.get(3, String.class);
                    }

                    result.put(k, v);

                    messageProcessed.incrementAndGet();
                    if (--remaining == 0)
                        break;
                }

                boolean wasLastPage = rs.getExecutionInfo().getPagingState() == null;
                if (wasLastPage) {
                    return Futures.immediateFuture(result);
                }
                else {
                    ListenableFuture<ResultSet> future = rs.fetchMoreResults();
                    return Futures.transform(future, iterate(result));
                }

            }
        };
    }


}
