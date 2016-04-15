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
    svn \
    && yum -y --nogpg -y clean all

RUN cd /export && echo "p" | svn export --force https://castagneto.socrate.vsct.fr/exploit/uxwadm

COPY hapctl_unif /HOME/uxwadm/scripts/hapctl_unif
COPY syslog-ng.persist /appl/syslogng/var/lib/syslog-ng.persist
RUN chmod +x /HOME/uxwadm/scripts/*



COPY HAP_KILL /HOME/hapadm/sudo/HAP_KILL
RUN chmod +x /HOME/hapadm/sudo/HAP_KILL

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Copy binary
COPY haaasd-linux_amd64 /haaasd

RUN mkdir -p /HOME/hapadm/SYSLOG/scripts && ln -s /HOME/uxwadm/scripts/haplogctl_unif /HOME/hapadm/SYSLOG/scripts/haplogctl

# Haproxy master script

COPY haproxy.cfg /conf/haproxy.cfg

RUN chown -R hapadm:haproxy /export/appl/hapadm
CMD /entrypoint.sh