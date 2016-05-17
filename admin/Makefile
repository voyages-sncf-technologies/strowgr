.PHONY: all docker-build docker-builder docker-image dist

BUILDDIR=target
BINARY=admin
IMAGE=haaas/$(BINARY)

VERSION=1.0.0

default: all

all: docker-builder docker-build docker-image

docker-builder:
	docker build -t $(IMAGE)-builder -f build.Dockerfile .

docker-build: docker-builder
	docker run --rm -v $(CURDIR)/target:/dist -v maven:/root/.m2 $(IMAGE)-builder

docker-image: dist
	cp docker/* dist
	docker build -t $(IMAGE):$(VERSION) dist

dist: target
	rm -fr dist && mkdir dist
	cp ${BUILDDIR}/admin-*.jar dist/app.jar

	
