# Build

```
$ GOPATH=$PWD go build sidekick
```

# Build with govvv

govvv is a tool for versioning binaries
 
```
$ export GOPATH=$PWD
$ go get github.com/ahmetalpbalkan/govvv
$ govvv build sidekick
$ ./sidekick -version
Version: 0.2.3-SNAPSHOT
Build date: 2016-12-01T22:18:43Z
GitCommit: 290f97d
GitBranch: nomaven
GitState: dirty
```


# Run


```
$ ./sidekick -h
Usage of ./sidekick:
  -config string
    	Configuration file (default "sidekick.conf")
  -fake string
    	Force response without reload for testing purpose. 'yesman': always say ok, 'drunk': random status/errors for entrypoint updates. Just for test purpose.
  -log-compact
    	compacting log
  -mono
    	only one haproxy instance which play slave/master roles.
  -verbose
    	Log in verbose mode
  -version
    	Print current version
```
