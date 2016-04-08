.PHONY: webapp haaasd docs haaas-registrator

all: webapp haaasd haaas-registrator

webapp:
	cd webapp && make docker-build && make docker-image

haaasd:
	cd haaasd && make docker-build && make docker-image

haaas-registrator:
	cd haaas-registrator && make docker-build && make docker-image

docs:
	docker run --rm -v $(CURDIR)/docs:/docs dockerregistry.socrate.vsct.fr:5000/dt/plantuml /docs/*.puml