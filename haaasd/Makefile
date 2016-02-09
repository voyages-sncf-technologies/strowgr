.PHONY: all test clean build install dist

BUILDDIR=bin
BINARY=haaasd
IMAGE=haaas/haaasd

VERSION=1.0.0

GOFLAGS ?= $(GOFLAGS:) -a -installsuffix cgo

all: build

build: cmd/main.go
#	CGO_ENABLED=0 GOOS=darwin go build $(GOFLAGS) -o ${BUILDDIR}/${BINARY}-darwin ./cmd/main.go
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(GOFLAGS) -o ${BUILDDIR}/${BINARY}-linux_amd64 ./cmd/main.go
#	CGO_ENABLED=0 GOOS=linux GOARCH=386 go build $(GOFLAGS) -o ${BUILDDIR}/${BINARY}-linux ./cmd/main.go

docker-build:
	docker build -t haaasd-builder -f build.Dockerfile .
	docker run -v $(CURDIR)/bin:/go/src/gitlab.socrate.vsct.fr/dt/haaasd/bin haaasd-builder make

docker-image: dist
	cp docker/* dist
	docker build -t $(IMAGE) dist

#webapp:
#	CGO_ENABLED=0 GOOS=darwin go build $(GOFLAGS) -o ${BUILDDIR}/${BINARY}-darwin ./cmd

dcup:
	docker-compose -f integration/docker-compose.yml up -d

dcdown:
	docker-compose -f integration/docker-compose.yml down

dclogs:
	docker-compose -f integration/docker-compose.yml logs

#docker-push:
#	docker push $(IMAGE)
#
#docker-all: docker-build docker-image docker-push
#
#
#run-swarm:
#	docker-compose -f test.docker-compose.yml up -d
#
#run:
#	 DOCKER_HOST=192.168.99.100:3376 go run swarm-demo.go -assets=assets
#
#logs:
#	docker-compose -f test.docker-compose.yml  logs
#
dist: ${BUILDDIR}/${BINARY}-linux_amd64
	rm -fr dist && mkdir dist
	cp ${BUILDDIR}/${BINARY}-linux_amd64 dist

clean:
	rm -fr {dist,bin}