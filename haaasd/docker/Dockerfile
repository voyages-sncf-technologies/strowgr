FROM dockerregistrydev.socrate.vsct.fr/vsct/centos:6
MAINTAINER VSCT DT PRODUIT <VSCTDTPRODUIT@voyages-sncf.com>
RUN yum install --enablerepo=EPEL -y \
    vsc_sudoers \
    vsc_haproxy_1.4.26 \
    vsc_haproxy_1.5.11 \
    vsc_haproxy_ssl1.4.22 \
    vsc_haproxy_ssl1.5.11 \
    vsc_haproxy_ssl1.5.14 \
    vsc_syslog-ng-3.3.5 \
    && yum -y --nogpg -y clean all

# Copy binary
COPY haaasd-linux_amd64 /haaasd

# Haproxy master script
COPY entrypoint.sh /entrypoint.sh
COPY haproxy.cfg /conf/haproxy.cfg
COPY hapctl_unif /HOME/uxwadm/scripts/hapctl_unif
COPY HAP_KILL /HOME/hapadm/sudo/HAP_KILL

# Default syslog frame
COPY syslog-ng.conf /HOME/hapadm/SYSLOG/Config/syslog.conf

RUN chmod +x /HOME/hapadm/sudo/HAP_KILL
RUN chmod +x /entrypoint.sh

CMD /entrypoint.sh