REM script de chargement de données: sous ensemble de la production: à enrichir si besoin est
REM Add curl to PATH
SET CURL_HOME=TODO
SET PATH=%PATH%;%CURL_HOME%/bin

curl -X PUT -d "[\"1.4.22\",\"1.4.26\",\"ssl1.5.14\"]" http://localhost:8500/v1/kv/haproxyversions

curl -X PUT -d "true" http://localhost:8500/v1/kv/haproxy/dmztech/autoreload
curl -X PUT -d "production" http://localhost:8500/v1/kv/haproxy/dmztech/platform
curl -X PUT -d "hapdmztechp-vip01.socrate.vsct.fr" http://localhost:8500/v1/kv/haproxy/dmztech/binding/0
curl -X PUT -d "dmztech" http://localhost:8500/v1/kv/haproxy/dmztech/name

curl -X PUT -d "true" http://localhost:8500/v1/kv/haproxy/horsprod/autoreload
curl -X PUT -d "preproduction" http://localhost:8500/v1/kv/haproxy/horsprod/platform
curl -X PUT -d "capena-vip01.socrate.vsct.fr" http://localhost:8500/v1/kv/haproxy/horsprod/binding/0
curl -X PUT -d "horsprod" http://localhost:8500/v1/kv/haproxy/horsprod/name

curl -X PUT -d "true" http://localhost:8500/v1/kv/haproxy/prod-lille/autoreload
curl -X PUT -d "production" http://localhost:8500/v1/kv/haproxy/prod-lille/platform
curl -X PUT -d "10.98.50.185" http://localhost:8500/v1/kv/haproxy/prod-lille/binding/0
curl -X PUT -d "prod-lille" http://localhost:8500/v1/kv/haproxy/prod-lille/name

curl -X PUT -d "true" http://localhost:8500/v1/kv/admin/ADATE/PRD1/autoreload
curl -X PUT -d "{\"haproxy\":\"dmztech\",\"hapUser\":\"hapadm\",\"hapVersion\":\"1.4.22\",\"bindingId\":0,\"frontends\":[{\"id\":\"ADATE\",\"context\":{}}],\"backends\":[{\"id\":\"adate-documentation\",\"servers\":[{\"id\":\"USLDORU11-ADATEDOC\",\"hostname\":\"USLDORU11-ADATEDOC\",\"ip\":\"dormelletto\",\"port\":\"58083\",\"context\":{},\"contextOverride\":{}}],\"context\":{}},{\"id\":\"adate-ui\",\"servers\":[{\"id\":\"USLDORU11-ADATEUI\",\"hostname\":\"USLDORU11-ADATEUI\",\"ip\":\"dormelletto\",\"port\":\"58082\",\"context\":{},\"contextOverride\":{}}],\"context\":{}},{\"id\":\"maestro\",\"servers\":[{\"id\":\"USLDORU11-ADATEMAESTRO\",\"hostname\":\"USLDORU11-ADATEMAESTRO\",\"ip\":\"dormelletto\",\"port\":\"58080\",\"context\":{},\"contextOverride\":{}}],\"context\":{}}],\"context\":{\"templateUri\":\"http://gitlab.socrate.vsct.fr/dt/haproxy-templates-horsprod/raw/master/ADATE/haproxy_template.conf\",\"application\":\"ADATE\",\"platform\":\"PRD1\"}}" http://localhost:8500/v1/kv/admin/ADATE/PRD1/current
curl -X PUT -d "" http://localhost:8500/v1/kv/admin/ADATE/PRD1/lock

curl -X PUT -d "true" http://localhost:8500/v1/kv/admin/AMK/INT1/autoreload
curl -X PUT -d "{\"haproxy\":\"horsprod\",\"hapUser\":\"hapadm\",\"hapVersion\":\"1.4.22\",\"bindingId\":0,\"frontends\":[{\"id\":\"AMKONGGTW\",\"context\":{}}],\"backends\":[{\"id\":\"AMKONGGTW\",\"servers\":[{\"id\":\"AMKMAGI1KONG_AMKONGGTW\",\"hostname\":\"AMKMAGI1KONG_AMKONGGTW\",\"ip\":\"maglione.socrate.vsct.fr\",\"port\":\"58000\",\"context\":{},\"contextOverride\":{}},{\"id\":\"AMKTRUI1KONG_AMKONGGTW\",\"hostname\":\"AMKTRUI1KONG_AMKONGGTW\",\"ip\":\"trusella.socrate.vsct.fr\",\"port\":\"58000\",\"context\":{},\"contextOverride\":{}}],\"context\":{}}],\"context\":{\"templateUri\":\"http://gitlab.socrate.vsct.fr/dt/haproxy-templates-horsprod/raw/master/AMK/haproxy_template.conf\",\"application\":\"AMK\",\"platform\":\"INT1\"}}" http://localhost:8500/v1/kv/admin/AMK/INT1/current
curl -X PUT -d "" http://localhost:8500/v1/kv/admin/AMK/INT1/lock

curl -X PUT -d "true" http://localhost:8500/v1/kv/admin/CLN/PRD1/autoreload
curl -X PUT -d "{\"haproxy\":\"prod-lille\",\"hapUser\":\"hapadm\",\"hapVersion\":\"ssl1.5.14\",\"bindingId\":0,\"frontends\":[{\"id\":\"AMKONGGTW\",\"context\":{}},{\"id\":\"AMKONGADM\",\"context\":{}}],\"backends\":[{\"id\":\"AMKONGADM\",\"servers\":[{\"id\":\"AMKPIAP1KONGADM\",\"hostname\":\"AMKPIAP1KONGADM\",\"ip\":\"PIARIO\",\"port\":\"58001\",\"context\":{},\"contextOverride\":{}},{\"id\":\"AMKBUSP1KONGADM\",\"hostname\":\"AMKBUSP1KONGADM\",\"ip\":\"BUSSO\",\"port\":\"58001\",\"context\":{},\"contextOverride\":{}}],\"context\":{}},{\"id\":\"AMKONGGTW\",\"servers\":[{\"id\":\"AMKPIAP1KONGGTW\",\"hostname\":\"AMKPIAP1KONGGTW\",\"ip\":\"PIARIO\",\"port\":\"58000\",\"context\":{},\"contextOverride\":{}},{\"id\":\"AMKBUSP1KONGGTW\",\"hostname\":\"AMKBUSP1KONGGTW\",\"ip\":\"BUSSO\",\"port\":\"58000\",\"context\":{},\"contextOverride\":{}}],\"context\":{}}],\"context\":{\"templateUri\":\"http://gitlab.socrate.vsct.fr/dt/haproxy-templates-horsprod/raw/master/AMK/haproxy_template.conf\",\"application\":\"AMK\",\"platform\":\"PRD1\"}}" http://localhost:8500/v1/kv/admin/CLN/PRD1/current
curl -X PUT -d "" http://localhost:8500/v1/kv/admin/CLN/PRD1/lock