.PHONY: sidekick registrator

all: sidekick registrator

sidekick:
	cd haaasd && make docker-build && make docker-image

registrator:
	cd haaas-registrator && make docker-build && make docker-image

