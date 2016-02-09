.PHONY: all test clean build install dist

BUILDDIR=bin
BINARY=webapp
IMAGE=haaas/$(BINARY)

VERSION=1.0.0

GOFLAGS ?= $(GOFLAGS:) -a -installsuffix cgo

all: build

build: main.go
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(GOFLAGS) -o ${BUILDDIR}/${BINARY}-linux_amd64 main.go

docker-build:
	docker build -t $(IMAGE)-builder -f build.Dockerfile .
	docker run -v $(CURDIR)/bin:/go/src/gitlab.socrate.vsct.fr/dt/haaas/bin $(IMAGE)-builder make

docker-image:
	cp Dockerfile dist
	docker build -t $(IMAGE):$(VERSION) dist

dist: ${BUILDDIR}/${BINARY}-linux_amd64
	rm -fr dist && mkdir dist
	cp ${BUILDDIR}/${BINARY}-linux_amd64 dist

clean:
	rm -fr {dist,bin}