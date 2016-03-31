package haaasd

import (
	log "github.com/Sirupsen/logrus"
	"os/exec"
	"fmt"
	"text/template"
	"os"
)

const syslogBaseConf = `
@version: 3.3

options {
  flush_lines (0);
  time_reopen (10);
  chain_hostnames (off);
};

filter f_local0 { facility(local0); };
filter f_syslog { level(info..emerg); };

# Conf pour la centralisation des logs HAP #
//destination d_haproxy_centralisation_pao_log {udp(10.101.149.21 port(63403)); };
//destination d_haproxy_centralisation_all_log {udp(10.101.149.21 port(55000)); };

# SYSLOG
source s_syslog { internal(); };
destination d_syslog { file("/HOME/hapadm/SYSLOG/logs/syslog.log"); };
log { source(s_syslog); filter (f_syslog); destination(d_syslog); };

@include "{{.HapHome}}/*/Config/syslog*.conf"
`

func NewSyslog(properties *Config) *Syslog {
	return &Syslog{
		properties: properties,
	}
}

type Syslog struct {
	properties *Config
}

// restart calls external shell script to reload syslog
// It returns error if the reload fails
func (syslog *Syslog) Restart() error {

	syslogCtl := fmt.Sprintf("%s/SYSLOG/scripts/haplogctl", syslog.properties.HapHome)
	cmd, err := exec.Command("sh", syslogCtl, "restart").Output()
	if err != nil {
		log.WithField("script", syslogCtl).WithField("cmd", cmd).WithError(err).Error("Error restarting syslog")
	}
	log.Debug("Syslog restarted")
	return err
}

// Init write the frame configuration
func (syslog *Syslog) Init() error {
	configFile := fmt.Sprintf("%s/SYSLOG/Config/syslog.conf", syslog.properties.HapHome)

	t := template.New("Syslog template")
	t, err := t.Parse(syslogBaseConf)
	if err != nil {
		log.Fatal(err)
	}

	f, err := os.OpenFile(configFile, os.O_WRONLY, 0644)
	if err != nil {
		log.WithError(err).Error("Fail to write base syslog file")
		return err
	}
	t.Execute(f, syslog.properties)
	log.WithField("filename", configFile).Debug("Syslog conf written")

	return nil
}
