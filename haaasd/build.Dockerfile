FROM golang:1.5

RUN go get github.com/tools/godep
RUN go get -u -v github.com/bitly/go-nsq

ENV SRC /go/src/gitlab.socrate.vsct.fr/dt/haaasd

RUN mkdir -p $SRC
WORKDIR $SRC

COPY . ./