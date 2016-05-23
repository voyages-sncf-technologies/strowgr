.PHONY: sidekick registrator

all: sidekick registrator

sidekick:
	cd sidekick && make docker-build && make docker-image

registrator:
	cd registrator && make docker-build && make docker-image

