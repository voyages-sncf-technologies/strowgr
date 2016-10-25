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
	// config of sidekick
	Config *Config
	// context of this haproxy (current application/platform/correlationid etc...)
	Context Context

	Filesystem Filesystem // directories managed by sidekick for this haproxy instance
	// command wrapping real command execution
	Command Command
	// dump abstraction to a file
	Dumper Dumper
	// signal abstraction for sending signal
	Signal Signal
}

func NewHaproxy(properties *Config, context Context) *Haproxy {
	// TODO manage a cache of Haproxy
	return &Haproxy{
		Config:     properties,
		Context:    context,
		Command:    execCommand,
		Signal:     osSignal,
		Dumper:     dumpConfiguration,
		Filesystem: NewFilesystem(properties.HapHome, context.Application, context.Platform),
	}
}

const (
	SUCCESS    int = iota
	UNCHANGED  int = iota
	ERR_SYSLOG int = iota
	ERR_CONF   int = iota
	ERR_RELOAD int = iota
	MAX_STATUS int = iota
)

// ApplyConfiguration write the new configuration and reload
// A rollback is called on failure
func (hap *Haproxy) ApplyConfiguration(event *EventMessageWithConf) (int, error) {
	fs := hap.Filesystem
	cmd := fs.Commands
	// validate version
	if event.Conf.Version == "" || !hap.isManagedVersion(event.Conf.Version) || event.Conf.Haproxy == nil {
		hap.Context.Fields(log.Fields{"given haproxy version": event.Conf.Version, "managed versions by sidekick": strings.Join(hap.Config.HapVersions, ",")}).Error("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance or configuration is missing")
		return ERR_CONF, errors.New("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance")
	}

	// create skeleton of directories
	fs.Mkdirs(hap.Context)

	// dump received haproxy configuration in debug mode
	hap.dumpDebug(event.Conf.Haproxy)

	// Check conf diff
	if cmd.Exists(fs.Files.ConfigFile) {
		// a configuration file already exists, should be archived
		if oldConf, err := cmd.Reader(fs.Files.ConfigFile); err != nil {
			return ERR_CONF, err
		} else if bytes.Equal(oldConf, event.Conf.Haproxy) {
			hap.Context.Fields(log.Fields{"id": hap.Config.Id}).Debug("Unchanged configuration")
			return UNCHANGED, nil
		} else {
			// Archive previous configuration
			if err := cmd.Renamer(fs.Files.ConfigFile, fs.Files.ConfigArchive); err != nil {
				hap.Context.Fields(log.Fields{"archivePath": fs.Files.ConfigArchive}).WithError(err).Error("can't archive config file")
				return ERR_CONF, err
			} else {
				hap.Context.Fields(log.Fields{"archivePath": fs.Files.ConfigArchive}).Debug("Old configuration archived")
			}
			// Archive previous binary (link)
			binOrigin, _ := cmd.ReadLinker(fs.Files.Binary)
			if err := cmd.Linker(binOrigin, fs.Files.BinaryArchive); err != nil {
				hap.Context.Fields(log.Fields{"bin archive": fs.Files.BinaryArchive}).WithError(err).Error("can't archive binary")
			} else {
				hap.Context.Fields(log.Fields{"archivePath": fs.Files.BinaryArchive}).Debug("Old bin archived")
			}
		}
	}

	// new version
	newVersion := fmt.Sprintf("/export/product/haproxy/product/%s/bin/haproxy", event.Conf.Version) // TODO externalize the binary path
	if err := cmd.Linker(newVersion, fs.Files.Binary); err != nil {
		hap.Context.Fields(log.Fields{"origin": newVersion, "destination": fs.Files.Binary}).WithError(err).Error("symlink failed")
	}
	if err := cmd.Writer(fs.Files.ConfigFile, event.Conf.Haproxy, 0644); err != nil {
		return ERR_CONF, err
	}

	hap.Context.Fields(log.Fields{"id": hap.Config.Id, "path": fs.Files.ConfigFile}).Info("New configuration written")

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
	if err := cmd.Writer(fs.Syslog.Path, event.Conf.Syslog, 0644); err != nil {
		hap.Context.Fields(log.Fields{"id": hap.Config.Id}).WithError(err).Error("Failed to write syslog fragment")
		// TODO Should we rollback on syslog error ?
		return ERR_SYSLOG, err
	}
	hap.Context.Fields(log.Fields{"id": hap.Config.Id, "content": string(event.Conf.Syslog), "filename": fs.Syslog}).Debug("Write syslog fragment")

	return SUCCESS, nil
}

// is a managed version by sidekick
func (hap *Haproxy) isManagedVersion(version string) bool {
	isManagedVersion := false
	for _, currentVersion := range hap.Config.HapVersions {
		if currentVersion == version {
			isManagedVersion = true
			break
		}
	}
	return isManagedVersion
}

func (hap *Haproxy) dumpDebug(newConf []byte) {
	if log.GetLevel() == log.DebugLevel {
		hap.Dumper(hap.Context, hap.Filesystem.Platform.Dump+"/"+time.Now().Format("20060102150405")+".log", newConf)
	}
}

func (hap *Haproxy) dumpError(newConf []byte) {
	hap.Dumper(hap.Context, hap.Filesystem.Platform.Errors+"/"+time.Now().Format("20060102150405")+".log", newConf)
}

// reload calls external shell script to reload haproxy
// It returns error if the reload fails
func (hap *Haproxy) reload(correlationId string) error {
	fs := hap.Filesystem
	cmd := fs.Commands
	configurationExists := cmd.Exists(fs.Files.PidFile)
	if configurationExists {
		pid, err := cmd.Reader(fs.Files.PidFile)
		if err != nil {
			hap.Context.Fields(log.Fields{"pid path": fs.Files.PidFile}).Error("can't read pid file")
			return err
		}
		hap.Context.Fields(log.Fields{"reloadScript": fs.Files.Binary, "confPath": fs.Files.ConfigFile, "pidPath": fs.Files.PidFile, "pid": strings.TrimSpace(string(pid))}).Debug("reload haproxy")

		if output, err := hap.Command(fs.Files.Binary, "-f", fs.Files.ConfigFile, "-p", fs.Files.PidFile, "-sf", strings.TrimSpace(string(pid))); err == nil {
			hap.Context.Fields(log.Fields{"id": hap.Config.Id, "reloadScript": fs.Files.Binary, "output": string(output[:])}).Debug("Reload succeeded")
		} else {
			hap.Context.Fields(log.Fields{"output": string(output[:])}).WithError(err).Error("Error reloading")
			return err
		}
	} else {
		hap.Context.Fields(log.Fields{"reloadScript": fs.Files.Binary, "confPath": fs.Files.ConfigFile, "pid file": fs.Files.PidFile}).Info("load haproxy")
		output, err := hap.Command(fs.Files.Binary, "-f", fs.Files.ConfigFile, "-p", fs.Files.PidFile)
		if err == nil {
			hap.Context.Fields(log.Fields{"id": hap.Config.Id, "reloadScript": fs.Files.Binary, "output": string(output[:])}).Debug("Reload succeeded")
		} else {
			hap.Context.Fields(log.Fields{"output": string(output[:])}).WithError(err).Error("Error reloading")
			return err
		}
	}
	return nil
}

// rollback reverts configuration files and call for reload
func (hap *Haproxy) rollback(correlationId string) error {
	fs := hap.Filesystem
	cmd := fs.Commands

	if err := cmd.Renamer(fs.Files.ConfigArchive, fs.Files.ConfigFile); err != nil {
		hap.Context.Fields(log.Fields{"archived config": fs.Files.ConfigArchive, "used config": fs.Files.ConfigFile}).WithError(err).Error("can't rename config archive to used config path")
		return err
	}

	if originBinArchived, err := cmd.ReadLinker(fs.Files.BinaryArchive); err != nil {
		hap.Context.Fields(log.Fields{"archived config": fs.Files.BinaryArchive}).WithError(err).Error("can't read origin of link to bin archive")
		return err
	} else {
		if err = cmd.Linker(originBinArchived, fs.Files.Binary); err != nil {
			hap.Context.Fields(log.Fields{"origin of link to binary": originBinArchived, "destination of link to binary": fs.Files.Binary}).WithError(err).Error("can't link binary to bin")
			return err
		} else {
			// success
			hap.Context.Fields(log.Fields{"origin of link to binary": originBinArchived, "destination of link to binary": fs.Files.Binary}).Debug("rollback of link to haproxy binary")
		}
	}

	return hap.reload(correlationId)
}

func (hap *Haproxy) Delete() error {
	fs := hap.Filesystem
	cmd := hap.Filesystem.Commands
	defer func() {
		if r := recover(); r != nil {
			hap.Context.Fields(log.Fields{"panic": r}).Error("Failed to delete haproxy configuration files or directories")
		}
	}()

	// remove bin and config files
	cmd.Remover(fs.Files.ConfigArchive)
	cmd.Remover(fs.Files.BinaryArchive)
	cmd.Remover(fs.Files.ConfigFile)
	cmd.Remover(fs.Files.Binary)
	cmd.Remover(fs.Platform.Path)
	return nil
}

func (hap *Haproxy) Stop() error {
	fs := hap.Filesystem
	cmd := hap.Filesystem.Commands
	pid, err := cmd.Reader(fs.Files.PidFile)
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
	return exec.Command(name, arg...).Output()
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

		context.Fields(log.Fields{"filename": filename}).Info("Dump configuration")
	}
}

func (hap Haproxy) Fake() bool {
	return false
}
