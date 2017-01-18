#!/usr/bin/env bash

cat > /etc/sidekick.conf <<-EOF
LookupdAddresses = ["$LOOKUP_ADDR"]
ProducerAddr = "$PRODUCER_ADDR"
ProducerRestAddr = "$PRODUCER_REST_ADDR"
ClusterId = "$CLUSTER_ID"
Port = $HTTP_PORT
HapHome = "$HAP_HOME"
Id = "$ID"
Status = "$STATUS"

[Hap."1.4.22"]
Path="/opt/haproxy-1.4.22"

[Hap."1.4.27"]
Path="/opt/haproxy-1.4.27"

[Hap."1.5.18"]
Path="/opt/haproxy-1.5.18"
EOF

echo "Starting sidekick with config: "
cat /etc/sidekick.conf

exec /sidekick -config=/etc/sidekick.conf $@