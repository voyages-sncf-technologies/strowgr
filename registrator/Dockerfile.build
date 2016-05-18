FROM dockerregistrydev.socrate.vsct.fr/golang:1.5

ENV SRC /go/src/gitlab.socrate.vsct.fr/dt/haaasd

RUN go get github.com/docker/engine-api
RUN go get github.com/vdemeester/docker-events
RUN go get github.com/Sirupsen/logrus
RUN go get github.com/samalba/dockerclient

RUN mkdir -p $SRC
WORKDIR $SRC

COPY . .
