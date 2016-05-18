.PHONY: all test clean build install dist

BUILDDIR=bin
BINARY=sidekick
IMAGE=strowgr/$(BINARY)

VERSION=1.0.0

GOFLAGS ?= $(GOFLAGS:) -a -installsuffix cgo

default: build

all: docker-build docker-image

generate:
	sed "s/{{ VERSION }}/$(VERSION)/" version.go.tpl >  $(CURDIR)/sidekick/src/version.go

build: src/cmd/sidekick.go
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(GOFLAGS) -o ${BUILDDIR}/${BINARY}-linux_amd64 src/cmd/sidekick.go

docker-builder:
	docker build -t $(IMAGE)-builder -f Dockerfile.build .

docker-build: docker-builder
	docker run --rm -e "CGO_ENABLED=0" -e "GOOS=linux" -e "GOARCH=amd64" -v $(CURDIR)/bin:/go/src/github.com/voyages-sncf-technologies/strowgr/sidekick/bin $(IMAGE)-builder go build $(GOFLAGS) -o ${BUILDDIR}/${BINARY}-linux_amd64 cmd/sidekick.go

docker-image: dist
	cp docker/* dist
	docker build -t $(IMAGE):$(VERSION) dist

docker-test: docker-builder
	docker run --rm $(IMAGE)-builder go test -v

test:
	go test -v

#Handle docker-compose
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

# Execute in the docker network

run: docker-builder
	docker run --rm -ti --net strowgr_default \
		-v $(CURDIR)/../data/slave/sidekick.conf:/sidekick.conf \
		-v $(CURDIR)/../data/slave/hapadm:/HOME/hapadm \
		$(IMAGE)-builder go run ./src/sidekick.go -config /sidekick.conf -ip local
