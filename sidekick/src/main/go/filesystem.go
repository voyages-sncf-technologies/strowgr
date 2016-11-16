package sidekick

import (
	"os"
	"strings"
	log "github.com/Sirupsen/logrus"
)

type Filesystem struct {
	Home        string
	Application Application
	Platform    Platform
	Syslog      SyslogDir
	Commands    Commands
	Files       AllFiles
}

type AllFiles struct {
	ConfigFile     string
	SyslogFile     string
	PidFile        string
	ConfigArchive  string
	Binary         string
	BinaryArchive  string
	Version        string
	VersionArchive string
}

type Application struct {
	Path     string
	Config   string
	Scripts  string
	Archives string
}

type Platform struct {
	Path   string
	Logs   string
	Errors string
	Dump   string
}

type SyslogDir struct {
	Path   string
	Config string
	Logs   string
}

// create a new filesystem based on user home, application and platform
func NewFilesystem(home, application, platform string) Filesystem {
	filesystem := &Filesystem{
		Home: home,
		Syslog: SyslogDir{
			Path:join(home, "SYSLOG", "Config", "syslog.conf.d"),
			Config:join(home, "SYSLOG", "Config"),
			Logs:join(home, "SYSLOG", "logs"),
		},
		Application: Application{
			Path:join(home, application),
			Config:join(home, application, "Config"),
			Scripts:join(home, application, "scripts"),
			Archives:join(home, application, "version-1"),
		},
		Platform: Platform{
			Path:join(home, application, platform),
			Logs:join(home, application, "logs", application + platform),
			Errors:join(home, application, platform, "errors"),
			Dump:join(home, application, platform, "dump"),
		},
		Commands: OsCommands{},
	}
	files := AllFiles{
		ConfigFile: join(filesystem.Application.Config, "hap" + application + platform + ".conf"),
		ConfigArchive: join(filesystem.Application.Archives, "hap" + application + platform + ".conf"),
		PidFile: join(filesystem.Platform.Logs, "haproxy.pid"),
		SyslogFile: join(filesystem.Syslog.Path, "syslog" + application + platform + ".conf"),
		Binary: join(filesystem.Application.Scripts, "hap" + application + platform),
		BinaryArchive: join(filesystem.Application.Archives, "hap" + application + platform),
		Version:join(filesystem.Platform.Path, "VERSION"),
		VersionArchive:join(filesystem.Application.Archives, "VERSION_" + platform),
	}
	filesystem.Files = files
	return *filesystem
}

// create all given directories
func (fs *Filesystem) Mkdirs(context Context) {
	defer func() {
		if r := recover(); r != nil {
			context.Fields(log.Fields{"recovery":r}).Error("can't create a directory")
		}
	}()
	// create application directories
	fs.Commands.MkdirAll(fs.Application.Path)
	fs.Commands.MkdirAll(fs.Application.Config)
	fs.Commands.MkdirAll(fs.Application.Scripts)
	fs.Commands.MkdirAll(fs.Application.Archives)
	// create platform directories
	fs.Commands.MkdirAll(fs.Platform.Path)
	fs.Commands.MkdirAll(fs.Platform.Dump)
	fs.Commands.MkdirAll(fs.Platform.Errors)
	fs.Commands.MkdirAll(fs.Platform.Logs)
	// create syslog directories
	fs.Commands.MkdirAll(fs.Syslog.Path)
	fs.Commands.MkdirAll(fs.Syslog.Config)
	fs.Commands.MkdirAll(fs.Syslog.Logs)

	context.Fields(log.Fields{}).Debug("all directories have been created")
}

// join path elements with os separators
func join(paths... string) string {
	return strings.Join(paths, string(os.PathSeparator))
}


