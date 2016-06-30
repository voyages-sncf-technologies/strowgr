FROM busybox

LABEL application.name "StrowgrSidekick"
LABEL application.desc "Strowgr docker sidekick"

COPY ${project.build.finalNameWithExt} /app
RUN chmod +x /app
ENTRYPOINT [ "/app" ]