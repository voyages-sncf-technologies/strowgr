Exemple de fichier de configuration pour la gui  

server:
  type: simple
  rootPath: /api/
  applicationContextPath: /
threads: 20
haproxyName: default-name
repository:
  host: 192.168.99.100
  port: 8500
nsqLookup:
  host: 192.168.99.100
  port: 4161
nsqProducer:
  host: 192.168.99.100
  port: 4150
  topic: commit_requested_
commitMessageConsumer:
  successTopic: commit_complete_
registerServerMessageConsumer:
  topic: register_server
periodicScheduler:
  current:
    periodMilli: 10000
  pending:
    periodMilli: 10000
logging:
  level: INFO
