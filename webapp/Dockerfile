FROM alpine

LABEL application.name TST
LABEL application.desc "Some application description"
LABEL service.50080_tcp.name WEBAPP
LABEL service.50080_tcp.desc "Some service description"

RUN apk -U add curl
COPY webapp-linux_amd64 /app
COPY BOOT /BOOT
RUN chmod +x /BOOT
ENTRYPOINT [ "/app" ]
CMD -instance $(hostname -i)
EXPOSE 50080

