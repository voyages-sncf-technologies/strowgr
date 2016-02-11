.PHONY: all test clean build install dist

BUILDDIR=bin
BINARY=haaasd
IMAGE=haaas/$(BINARY)

VERSION=1.0.0

GOFLAGS ?= $(GOFLAGS:) -a -installsuffix cgo

default: build

all: docker-build docker-image

generate_version:
	echo "package haaasd\nconst AppVersion = \"$(VERSION)\"" > $(CURDIR)/src/haaasd/version.go

build: src/haaasd.go
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(GOFLAGS) -o ${BUILDDIR}/${BINARY}-linux_amd64 ./src/haaasd.go

docker-build:
	docker build -t $(IMAGE)-builder -f build.Dockerfile .
	docker run -v $(CURDIR)/bin:/go/src/gitlab.socrate.vsct.fr/dt/haaas/bin $(IMAGE)-builder make

docker-image: dist
	cp docker/* dist
	docker build -t $(IMAGE) dist

dcup:
	docker-compose -f integration/docker-compose.yml up -d

dcdown:
	docker-compose -f integration/docker-compose.yml down

dclogs:
	docker-compose -f integration/docker-compose.yml logs

dist: ${BUILDDIR}/${BINARY}-linux_amd64
	rm -fr dist && mkdir dist
	cp ${BUILDDIR}/${BINARY}-linux_amd64 dist

clean:
	rm -fr {dist,bin}