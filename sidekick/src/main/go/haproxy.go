/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package sidekick

import (
	"bytes"
	"errors"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"syscall"
	"time"
)

// Haproxy manager for a given Application/Platform
type Haproxy struct {
	Config     *Config    // config of sidekick
	Context    Context    // context of this haproxy (current application/platform/correlationid etc...)
	Filesystem Filesystem // filesystem with haproxy configuration files
	Command    Command    // wrapping os command execution
	Dumper     Dumper     // wrapping for dumping contents on files
	Signal     Signal     // signal abstraction for sending signal
}

// create a new haproxy
func NewHaproxy(config *Config, context Context) *Haproxy {
	// TODO manage a cache of Haproxy
	return &Haproxy{
		Config:     config,
		Context:    context,
		Command:    execCommand,
		Signal:     osSignal,
		Dumper:     dumpConfiguration,
		Filesystem: NewFilesystem(config.HapHome, context.Application, context.Platform),
	}
}

// status of applying a new configuration
const (
	SUCCESS    int = iota // configuration application has succeed
	UNCHANGED  int = iota // configuration has not been changed
	ERR_SYSLOG int = iota // error during syslog configuration change
	ERR_CONF   int = iota // error with given configuration
	ERR_RELOAD int = iota // error during haproxy reload
	MAX_STATUS int = iota // technical status for enumerating status
)

// ApplyConfiguration write the new configuration and reload
// A rollback is called on failure
func (hap *Haproxy) ApplyConfiguration(event *EventMessageWithConf) (status int, err error) {
	fs := hap.Filesystem
	cmd := fs.Commands

	defer func() {
		if r := recover(); r != nil {
			// if panic during io executions, return status
			switch r.(type) {
			case error:
				hap.Context.Fields(log.Fields{}).WithError(r.(error)).Error("error on configuration")
				status, err = ERR_CONF, r.(error)
			default:
				hap.Context.Fields(log.Fields{"recover": r}).Error("error on configuration")
				status, err = ERR_CONF, nil
			}
		} else {
			hap.Context.Fields(log.Fields{}).Debug("end of applying received configuration")
		}
	}()

	// validate event
	hap.validate(event)

	// create skeleton of directories
	fs.Mkdirs(hap.Context)

	// dump received haproxy configuration in verbose mode
	hap.dumpDebug(event.Conf.Haproxy)

	if cmd.Exists(fs.Files.ConfigFile) {
		hap.Context.Fields(log.Fields{"config": fs.Files.ConfigFile}).Debug("configuration file found")

		// configuration already exists for this haproxy
		oldConf, _ := cmd.Reader(fs.Files.ConfigFile, true)

		oldVersion, _ := cmd.Reader(fs.Files.Version, true)

		if bytes.Equal(oldConf, event.Conf.Haproxy) && string(oldVersion) == event.Conf.Version {
			// check and restart killed haproxy
			hap.restart_killed_haproxy()

			// unchanged configuration file
			hap.Context.Fields(log.Fields{"id": hap.Config.Id}).Debug("unchanged configuration")
			return UNCHANGED, nil
		}

		// Archive previous configuration
		hap.archiveConfigurationFile()

		// Archive previous binary (link)
		hap.archiveBinary()
	} else {
		hap.Context.Fields(log.Fields{}).Info("create configuration file for the first time")
	}

	// override link to new version (if needed)
	cmd.Writer(fs.Files.Version, []byte(event.Conf.Version), 0644, true)
	newVersion := hap.Config.Hap[event.Conf.Version].Path + "/bin/haproxy"
	cmd.Linker(newVersion, fs.Files.Binary, true)
	hap.Context.Fields(log.Fields{"version": newVersion, "path bin": fs.Files.Binary}).Debug("link to binary haproxy")

	// write new configuration file
	cmd.Writer(fs.Files.ConfigFile, event.Conf.Haproxy, 0644, true)
	hap.Context.Fields(log.Fields{"id": hap.Config.Id, "path": fs.Files.ConfigFile}).Info("write received configuration (first time or detected changes)")

	// Reload haproxy
	if err := hap.reload(event.Header.CorrelationId); err != nil {
		hap.Context.Fields(log.Fields{"id": hap.Config.Id}).WithError(err).Error("Reload failed")
		hap.dumpError(event.Conf.Haproxy)
		if errRollback := hap.rollback(event.Header.CorrelationId); errRollback != nil {
			log.WithError(errRollback).Error("error in rollback in addition to error of the reload")
		} else {
			hap.Context.Fields(log.Fields{}).Debug("rollback done")
		}
		return ERR_RELOAD, err
	}

	// Write syslog fragment
	cmd.Writer(fs.Files.SyslogFile, event.Conf.Syslog, 0644, true)

	return SUCCESS, nil
}

func (hap *Haproxy) restart_killed_haproxy() error {
	hap.Context.Fields(log.Fields{}).Debug("check haproxy is up, restart otherwise")

	fs := hap.Filesystem
	cmd := fs.Commands
	if cmd.Exists(fs.Files.PidFile) {
		pid, err := cmd.Reader(fs.Files.PidFile, true)
		if err != nil || pid == nil || len(pid) == 0 {
			hap.Context.Fields(log.Fields{"pid path": fs.Files.PidFile, "pid": pid}).Error("can't read pid file")
			return err
		}

		if output, err := hap.Command("ps", "-p", strings.TrimSpace(string(pid))); err == nil {
			hap.Context.Fields(log.Fields{"id": hap.Config.Id, "output": string(output[:])}).Debug("process haproxy is running. nothing to do.")
			return nil
		} else {
			hap.Context.Fields(log.Fields{"output": string(output[:])}).WithError(err).Error("process haproxy is not running. attempt to restart it.")
			if err = hap.reload(hap.Context.CorrelationId); err != nil {
				hap.Context.Fields(log.Fields{}).WithError(err).Error("can't restart haproxy process")
			} else {
				hap.Context.Fields(log.Fields{}).Info("haproxy process restarted after detecting it was killed.")
			}
			return err
		}
	}
	return nil
}

// validate input event
func (hap *Haproxy) validate(event *EventMessageWithConf) error {
	if event.Conf.Version == "" || !hap.isManagedVersion(event.Conf.Version) || event.Conf.Haproxy == nil {
		hapVersions := make([]string, 0, len(hap.Config.Hap))
		for k := range hap.Config.Hap {
			hapVersions = append(hapVersions, k)
		}
		hap.Context.Fields(log.Fields{"given haproxy version": event.Conf.Version, "managed versions by sidekick": strings.Join(hapVersions, ",")}).Error("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance or configuration is missing")
		panic(errors.New("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance"))
	}
	return nil
}

// archive configuration file
func (hap *Haproxy) archiveConfigurationFile() {
	fs := hap.Filesystem
	hap.Filesystem.Commands.Renamer(fs.Files.ConfigFile, fs.Files.ConfigArchive, true)
	hap.Filesystem.Commands.Renamer(fs.Files.Version, fs.Files.VersionArchive, true)
	hap.Context.Fields(log.Fields{"archivePath": fs.Files.ConfigArchive}).Debug("old configuration files archived")
}

// archive binary
func (hap *Haproxy) archiveBinary() {
	fs := hap.Filesystem

	if binOrigin, err := hap.Filesystem.Commands.ReadLinker(fs.Files.Binary, true); err != nil {
		hap.Context.Fields(log.Fields{"bin archive": fs.Files.BinaryArchive, "bin": fs.Files.Binary}).WithError(err).Error("can't read link destination of binary")
		panic(err)
	} else if err := hap.Filesystem.Commands.Linker(binOrigin, fs.Files.BinaryArchive, true); err != nil {
		hap.Context.Fields(log.Fields{"bin archive": fs.Files.BinaryArchive}).WithError(err).Error("can't archive binary")
		panic(err)
	} else {
		hap.Context.Fields(log.Fields{"archivePath": fs.Files.BinaryArchive}).Debug("Old bin archived")
	}
}

// is a managed version by sidekick
func (hap *Haproxy) isManagedVersion(version string) bool {
	_, success := hap.Config.Hap[version]
	return success
}

// dump configuration to dump directory if debug level is enabled
func (hap *Haproxy) dumpDebug(config []byte) {
	if log.GetLevel() == log.DebugLevel {
		hap.Dumper(hap.Context, hap.Filesystem.Platform.Dump+"/"+time.Now().Format("20060102150405")+".log", config)
	}
}

// dump configuration to error directory
func (hap *Haproxy) dumpError(config []byte) {
	hap.Dumper(hap.Context, hap.Filesystem.Platform.Errors+"/"+time.Now().Format("20060102150405")+".log", config)
}

// reload calls external shell script to reload haproxy
// It returns error if the reload fails
func (hap *Haproxy) reload(correlationId string) error {
	fs := hap.Filesystem
	cmd := fs.Commands
	configurationExists := cmd.Exists(fs.Files.PidFile)
	if configurationExists {
		pid, err := cmd.Reader(fs.Files.PidFile, true)
		if err != nil || pid == nil || len(pid) == 0 {
			hap.Context.Fields(log.Fields{"pid path": fs.Files.PidFile, "pid": pid}).Error("can't read pid file")
			return err
		}
		hap.Context.Fields(log.Fields{"reloadScript": fs.Files.Binary, "confPath": fs.Files.ConfigFile, "pidPath": fs.Files.PidFile, "pid": strings.TrimSpace(string(pid))}).Debug("attempt reload haproxy command")
		if output, err := hap.Command(fs.Files.Binary, "-f", fs.Files.ConfigFile, "-p", fs.Files.PidFile, "-sf", strings.TrimSpace(string(pid))); err == nil {
			hap.Context.Fields(log.Fields{"id": hap.Config.Id, "reloadScript": fs.Files.Binary, "output": string(output[:])}).Debug("reload succeeded")
		} else {
			hap.Context.Fields(log.Fields{"output": string(output[:])}).WithError(err).Error("error reloading")
			return err
		}
	} else {
		hap.Context.Fields(log.Fields{"reloadScript": fs.Files.Binary, "confPath": fs.Files.ConfigFile, "pid file": fs.Files.PidFile}).Info("start haproxy for the first time")
		output, err := hap.Command(fs.Files.Binary, "-f", fs.Files.ConfigFile, "-p", fs.Files.PidFile)
		if err == nil {
			hap.Context.Fields(log.Fields{"id": hap.Config.Id, "reloadScript": fs.Files.Binary, "output": string(output[:])}).Info("success of the first haproxy start")
		} else {
			hap.Context.Fields(log.Fields{"output": string(output[:])}).WithError(err).Error("fail of the first haproxy start")
			return err
		}
	}
	return nil
}

// rollback reverts configuration files and call for reload
func (hap *Haproxy) rollback(correlationId string) error {
	fs := hap.Filesystem
	cmd := fs.Commands

	if err := cmd.Renamer(fs.Files.ConfigArchive, fs.Files.ConfigFile, false); err != nil {
		hap.Context.Fields(log.Fields{"archived config": fs.Files.ConfigArchive, "used config": fs.Files.ConfigFile}).WithError(err).Error("can't rename config archive to used config path")
		return err
	} else if err = cmd.Renamer(fs.Files.VersionArchive, fs.Files.Version, false); err != nil {
		hap.Context.Fields(log.Fields{"archived config": fs.Files.VersionArchive, "used config": fs.Files.Version}).WithError(err).Error("can't rename version archive to used version path")
		return err
	}

	if originBinArchived, err := cmd.ReadLinker(fs.Files.BinaryArchive, false); err != nil {
		hap.Context.Fields(log.Fields{"archived config": fs.Files.BinaryArchive}).WithError(err).Error("can't read origin of link to bin archive")
		return err
	} else {
		if err = cmd.Linker(originBinArchived, fs.Files.Binary, false); err != nil {
			hap.Context.Fields(log.Fields{"origin of link to binary": originBinArchived, "destination of link to binary": fs.Files.Binary}).WithError(err).Error("can't link binary to bin")
			return err
		} else {
			// success
			hap.Context.Fields(log.Fields{"origin of link to binary": originBinArchived, "destination of link to binary": fs.Files.Binary}).Debug("rollback of link to haproxy binary")
		}
	}

	return hap.reload(correlationId)
}

// delete configuration files
func (hap *Haproxy) Delete() error {
	hap.Context.Fields(log.Fields{}).Info("delete all files and directory for this entrypoint")

	fs := hap.Filesystem
	cmd := hap.Filesystem.Commands
	defer func() {
		if r := recover(); r != nil {
			hap.Context.Fields(log.Fields{"panic": r}).Error("failed to delete haproxy configuration files or directories")
		}
	}()

	// remove bin and config files
	cmd.Remover(fs.Files.ConfigArchive, false)
	cmd.Remover(fs.Files.BinaryArchive, false)
	cmd.Remover(fs.Files.ConfigFile, true)
	cmd.Remover(fs.Files.Binary, true)
	cmd.Remover(fs.Platform.Path, true)
	cmd.Remover(fs.Platform.Logs, true)
	return nil
}

// stop haproxy process
func (hap *Haproxy) Stop() error {
	hap.Context.Fields(log.Fields{}).Info("stop haproxy process")
	fs := hap.Filesystem
	cmd := hap.Filesystem.Commands
	pid, err := cmd.Reader(fs.Files.PidFile, false)
	if err != nil {
		hap.Context.Fields(log.Fields{"pid file": fs.Files.PidFile}).Error("can't read pid file")
	}
	pidInt, err := strconv.Atoi(strings.TrimSpace(string(pid)))
	if err != nil {
		hap.Context.Fields(log.Fields{"pid": strings.TrimSpace(string(pid)), "pid file": fs.Files.PidFile}).Error("can't convert pid to int")
		return err
	}
	err = hap.Signal(pidInt, syscall.SIGTERM)

	if err != nil {
		hap.Context.Fields(log.Fields{}).WithError(err).Error("SIGTERM sent to haproxy process fails")
	}
	return err
}

/////////////////////////
//         types       //
/////////////////////////

// type abstraction for exec.Command(...).Output()
type Command func(name string, arg ...string) ([]byte, error)

// type abstraction for os.Signal
type Signal func(pid int, signal os.Signal) error

// type abstraction for dumping content to a file
type Dumper func(context Context, filename string, newConf []byte)

/////////////////////////
// OS implementations  //
/////////////////////////

func execCommand(name string, arg ...string) ([]byte, error) {
	return exec.Command(name, arg...).CombinedOutput()
}

func osSignal(pid int, signal os.Signal) error {
	process, err := os.FindProcess(pid)
	if err != nil {
		return err
	}
	return process.Signal(signal)
}

// dumpConfiguration dumps the new configuration file with context for debugging purpose
func dumpConfiguration(context Context, filename string, newConf []byte) {
	f, err2 := os.Create(filename)
	defer f.Close()
	if err2 == nil {
		f.WriteString("================================================================\n")
		f.WriteString(fmt.Sprintf("application: %s\n", context.Application))
		f.WriteString(fmt.Sprintf("platform: %s\n", context.Platform))
		f.WriteString(fmt.Sprintf("correlationId: %s\n", context.CorrelationId))
		f.WriteString("================================================================\n")
		f.Write(newConf)
		f.Sync()

		context.Fields(log.Fields{"filename": filename}).Debug("dump configuration to filesystem")
	}
}

func (hap Haproxy) Fake() bool {
	return false
}
