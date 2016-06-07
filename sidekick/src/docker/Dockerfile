FROM busybox

LABEL application.name "StrowgrRegistrator"
LABEL application.desc "Strowgr docker registrator"

COPY ${project.build.finalNameWithExt} /app
RUN chmod 700 /app

ENTRYPOINT [ "/app" ]