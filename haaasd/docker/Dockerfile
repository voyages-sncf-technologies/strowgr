FROM haproxy:1.5

COPY haaasd-linux_amd64 /haaasd
COPY entrypoint.sh /entrypoint.sh
COPY haproxy.cfg /conf/haproxy.cfg
RUN chmod +x /entrypoint.sh
#CMD /haaasd --config /haaas.conf -ip $(ip addr show dev eth0|grep -o -e "[0-9][0-9]*.[0-9][0-9]*.[0-9][0-9]*.[0-9][0-9]*")
CMD /entrypoint.sh