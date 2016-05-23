package sidekick

import (
	log "github.com/Sirupsen/logrus"
	"os/exec"
	"fmt"
	"text/template"
	"os"
	"time"
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
	output, err := exec.Command("sh", syslogCtl, "stop").Output()
	if err != nil {
		log.WithFields(SyslogFields()).WithField("script", syslogCtl).WithField("output", string(output)).WithError(err).Error("can't stop syslog")
	} else {
		log.WithFields(SyslogFields()).WithField("output", string(output[:])).Info("Syslog stopped. Wait 1s before the restart.")
		time.Sleep(time.Duration(1) * time.Second)

		output, err = exec.Command("sh", syslogCtl, "start").Output()
		if err != nil {
			log.WithFields(SyslogFields()).WithField("script", syslogCtl).WithField("output", string(output)).WithError(err).Error("can't start syslog")
		} else {
			log.WithFields(SyslogFields()).WithField("output", string(output[:])).Info("Syslog started")

		}
	}
	return err
}

// Init write the frame configuration
func (syslog *Syslog) Init() error {
	configDir := fmt.Sprintf("%s/SYSLOG/Config", syslog.properties.HapHome)
	configFile := fmt.Sprintf("%s/syslog.conf", configDir)

	t := template.New("Syslog template")
	t, err := t.Parse(syslogBaseConf)
	if err != nil {
		log.WithFields(SyslogFields()).Fatal(err)
	}

	createDirectory(Context{}, "init", fmt.Sprintf("%s/SYSLOG/logs", syslog.properties.HapHome))
	createDirectory(Context{}, "init", configDir)

	f, err := os.OpenFile(configFile, os.O_CREATE | os.O_WRONLY, 0644)
	if err != nil {
		log.WithFields(SyslogFields()).WithError(err).Error("Fail to write base syslog file")
		return err
	}
	t.Execute(f, syslog.properties)
	log.WithFields(SyslogFields()).WithField("filename", configFile).Debug("Syslog conf written")

	return nil
}

func SyslogFields() log.Fields {
	return log.Fields{
		"timestamp": time.Now().UnixNano() / int64(time.Millisecond),
		"type": "syslog",
	}
}