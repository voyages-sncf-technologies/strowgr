.PHONY: all test clean build install

BUILDDIR=bin
BINARY=haaasd

VERSION=1.0.0

GOFLAGS ?= $(GOFLAGS:) -a -installsuffix cgo

all: build

build: haaasd.go
	CGO_ENABLED=0 GOOS=darwin go build $(GOFLAGS) -o ${BUILDDIR}/${BINARY}-darwin .
#	CGO_ENABLED=0 GOOS=linux go build $(GOFLAGS) -o ${BUILDDIR}/${BINARY}-linux .

docker-build:
	docker build -t haaasd-builder -f build.Dockerfile .
	docker run -v $(CURDIR)/bin:/go/src/gitlab.socrate.vsct.fr/dt/haaasd/bin haaasd-builder make

#docker-image: dist
#	cp Dockerfile dist
#	docker build -t $(IMAGE) dist
#
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
#dist: ${BUILDDIR}/${BINARY}
#	rm -fr dist && mkdir dist
#	cp ${BUILDDIR}/${BINARY} dist
#	cp -R assets dist/

clean:
	rm -fr dist