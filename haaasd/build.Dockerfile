FROM golang:1.5

RUN go get github.com/BurntSushi/toml/cmd/tomlv
RUN go get -u -v github.com/bitly/go-nsq

ENV SRC /go/src/gitlab.socrate.vsct.fr/dt/haaas

RUN mkdir -p $SRC
WORKDIR $SRC

COPY . ./