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

@include "{{.HapHome}}/SYSLOG/Config/syslog.conf.d/"

# SYSLOG
source s_syslog { internal(); };
destination d_syslog { file("/HOME/hapadm/SYSLOG/logs/syslog.log"); };
log { source(s_syslog); filter (f_syslog); destination(d_syslog); };

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
		log.WithField("script", syslogCtl).WithField("output", string(cmd)).WithError(err).Error("Error restarting syslog")
	}
	log.Debug("Syslog restarted")
	return err
}

// Init write the frame configuration
func (syslog *Syslog) Init() error {
	configDir := fmt.Sprintf("%s/SYSLOG/Config", syslog.properties.HapHome)
	configFile := fmt.Sprintf("%s/syslog.conf", configDir)

	t := template.New("Syslog template")
	t, err := t.Parse(syslogBaseConf)
	if err != nil {
		log.Fatal(err)
	}

	createDirectory(fmt.Sprintf("%s/SYSLOG/logs", syslog.properties.HapHome))
	createDirectory(configDir)

	f, err := os.OpenFile(configFile, os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		log.WithError(err).Error("Fail to write base syslog file")
		return err
	}
	t.Execute(f, syslog.properties)
	log.WithField("filename", configFile).Debug("Syslog conf written")

	return nil
}
