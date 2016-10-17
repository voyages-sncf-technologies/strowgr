package sidekick

import (
	"fmt"
	log "github.com/Sirupsen/logrus"
	"io/ioutil"
	"os"
)

type Reader func(path string) ([]byte, error)
type Writer func(path string, content []byte, perm os.FileMode) error
type Renamer func(oldpath, newpath string) error
type Checker func(path string) bool
type Linker func(oldpath, newpath string) error
type Remover func(path string) error
type ReadlLinker func(path string) (string,error)

type Files struct {
	Config        string
	Syslog        string
	ConfigArchive string
	BinArchive    string
	Pid           string
	Bin           string
	Context       Context
	Reader        Reader
	Writer        Writer
	Renamer       Renamer
	Checker       Checker
	Linker        Linker
	Remover       Remover
	ReadLinker   ReadlLinker
}

func NewPath(context Context, config, syslog, archive, pid, bin, binArchive string) Files {
	files := Files{
		Reader:  ioutil.ReadFile,
		Writer:  ioutil.WriteFile,
		Renamer: os.Rename,
		Checker: osExists,
		Linker:  os.Symlink,
		Remover: os.Remove,
		ReadLinker: os.Readlink,
	}
	files.Context = context
	files.ConfigArchive = archive
	files.Config = config
	files.Syslog = syslog
	files.Pid = pid
	files.Bin = bin
	files.BinArchive = binArchive
	return files
}

func (files Files) linkNewVersion(version string) error {
	newVersion := fmt.Sprintf("/export/product/haproxy/product/%s/bin/haproxy", version)
	if files.Checker(files.Bin) {
		files.Context.Fields(log.Fields{"file link": files.Bin}).Debug("remove existing link")
		files.Remover(files.Bin)
	}

	if err := files.Linker(newVersion, files.Bin); err != nil {
		files.Context.Fields(log.Fields{"path": newVersion}).WithError(err).Error("Symlink failed")
		return err
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
	if err := files.Renamer(files.Config, files.ConfigArchive); err != nil {
		files.Context.Fields(log.Fields{"archivePath": files.ConfigArchive}).WithError(err).Error("can't archive config file")
		return err
	} else {
		files.Context.Fields(log.Fields{"archivePath": files.ConfigArchive}).Debug("Old configuration archived")
	}

	if files.binArchiveExists() {
		files.Remover(files.BinArchive)
	}
	binDestination, _ := files.ReadLinker(files.Bin)
	if err := files.Linker(binDestination, files.BinArchive); err != nil {
		files.Context.Fields(log.Fields{"archivePath": files.BinArchive}).WithError(err).Error("can't archive bin file")
		return err
	} else {
		files.Context.Fields(log.Fields{"archivePath": files.BinArchive}).Debug("Old bin archived")
	}
	return nil
}

func (files Files) rollback() error {
	if err := files.Renamer(files.ConfigArchive, files.Config); err != nil {
		files.Context.Fields(log.Fields{"archived config":files.ConfigArchive, "used config":files.Config}).WithError(err).Error("can't rename config archive to used config path")
		return err
	}

	if err := files.Renamer(files.BinArchive, files.Bin); err != nil {
		files.Context.Fields(log.Fields{"archived bin":files.ConfigArchive, "used config":files.Config}).WithError(err).Error("can't rename config archive to used config path")
		return err
	}

	return nil
}

func (files Files) removeAll() error {
	err := files.Remover(files.Config)
	if err != nil {
		return err
	}
	return files.Remover(files.Bin)
}

func (files Files) configArchiveExists() bool {
	return files.Checker(files.ConfigArchive)
}

func (files Files) binArchiveExists() bool {
	return files.Checker(files.BinArchive)
}

func osExists(path string) bool {
	if _, err := os.Stat(path); err == nil {
		return true
	}
	return false
}
