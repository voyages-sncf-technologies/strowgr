package sidekick

import (
	"os"
	"io/ioutil"
	log "github.com/Sirupsen/logrus"
	"fmt"
)

type Reader func(path string) ([]byte, error)
type Writer func(path string, content []byte, perm os.FileMode) error
type Renamer func(oldpath, newpath string) error
type Checker func(path string) bool
type Linker func(oldpath, newpath string) error
type Remover func(path string) error

type Files struct {
	Config  string
	Syslog  string
	Archive string
	Pid     string
	Bin     string
	Context Context
	Reader  Reader
	Writer  Writer
	Renamer Renamer
	Checker Checker
	Linker  Linker
	Remover Remover
}

func NewPath(context Context, config, syslog, archive, pid, bin string) Files {
	files := Files{
		Reader: ioutil.ReadFile,
		Writer: ioutil.WriteFile,
		Renamer: os.Rename,
		Checker: osExists,
		Linker: os.Symlink,
		Remover: os.Remove,
	}
	files.Context = context
	files.Archive = archive
	files.Config = config
	files.Syslog = syslog
	files.Pid = pid
	files.Bin = bin
	return files
}

func (files Files) linkNewVersion(version string) error {
	newVersion := fmt.Sprintf("/export/product/haproxy/product/%s/bin/haproxy", version)
	newLink := true
	if files.Checker(files.Bin) {
		files.Remover(files.Bin)
		newLink = false
	}
	err := files.Linker(newVersion, files.Bin)
	if err != nil {
		files.Context.Fields(log.Fields{"path": newVersion}).WithError(err).Error("Symlink failed")
		return err
	}

	if newLink {
		files.Context.Fields(log.Fields{"path": newVersion}).WithError(err).Error("Symlink created")
	}
	return nil
}

func (files Files) readConfig() ([]byte, error) {
	return files.Reader(files.Config)
}

func (files Files) readPid() ([]byte, error) {
	return files.Reader(files.Pid)
}

func (files Files) writeConfig(content []byte) error {
	return files.Writer(files.Config, content, 0644)
}

func (files Files) writeSyslog(content []byte) error {
	return files.Writer(files.Syslog, content, 0644)
}

func (files Files) archive() error {
	return files.Renamer(files.Config, files.Archive)
}

func (files Files) rollback() error {
	return files.Renamer(files.Archive, files.Config)
}

func (files Files) removeAll() error {
	err := files.Remover(files.Config)
	if err != nil {
		return err
	}
	return files.Remover(files.Bin)
}

func (files Files) archiveExists() bool {
	return files.Checker(files.Archive)
}

func osExists(path string) bool {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return true
	}
	return false
}
