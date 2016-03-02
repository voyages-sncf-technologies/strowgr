FROM alpine
RUN apk -U add curl
COPY webapp-linux_amd64 /app
COPY BOOT /BOOT
RUN chmod +x /BOOT
ENTRYPOINT [ "/BOOT" ]
