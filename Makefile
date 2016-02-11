.PHONY: webapp haaasd

all: webapp haaasd

webapp:
	cd webapp && make docker-build && make docker-image

haaasd:
	cd haaasd && make docker-build && make docker-image
