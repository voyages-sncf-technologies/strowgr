FROM dockerregistry.socrate.vsct.fr:5000/vsct/centos:6
RUN yum install -y \
    vsc_sudoers \
    vsc_haproxy_1.4.26 \
    vsc_haproxy_1.4.26 \
    vsc_haproxy_1.5.11 \
    vsc_haproxy_ssl1.4.22 \
    vsc_haproxy_ssl1.5.11 \
    vsc_haproxy_ssl1.5.14 \
    && yum -y --nogpg -y clean all
COPY haaasd-linux_amd64 /haaasd
COPY entrypoint.sh /entrypoint.sh
COPY haproxy.cfg /conf/haproxy.cfg
COPY hapctl_unif /HOME/uxwadm/scripts/hapctl_unif
RUN chmod +x /entrypoint.sh

CMD /entrypoint.sh