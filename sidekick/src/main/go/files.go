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
type ReadlLinker func(path string) (string, error)

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
	ReadLinker    ReadlLinker
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

	if err := files.Linker(newVersion, files.Bin); err != nil {
		if os.IsExist(err) {
			if err := files.Remover(files.Bin); err != nil {
				files.Context.Fields(log.Fields{"path": files.Bin}).WithError(err).Error("can't remove link")
			} else {
				// retry linking
				if errLinker := files.Linker(newVersion, files.Bin); errLinker != nil {
					files.Context.Fields(log.Fields{"origin": newVersion, "destination":files.Bin}).WithError(err).Error("symlink failed")
				}
			}
		} else {
			files.Context.Fields(log.Fields{"origin": newVersion, "destination":files.Bin}).WithError(err).Error("symlink failed")
			return err
		}
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

	binOrigin, _ := files.ReadLinker(files.Bin)
	if err := files.Linker(binOrigin, files.BinArchive); err != nil {
		if os.IsExist(err) {
			if err = files.Remover(files.BinArchive); err != nil {
				files.Context.Fields(log.Fields{"bin archive":files.BinArchive}).WithError(err).Error("can't remove bin archive")
			} else if err = files.Linker(binOrigin, files.BinArchive); err != nil {
				files.Context.Fields(log.Fields{"bin archive":files.BinArchive, "origin link":binOrigin}).WithError(err).Error("can't create bin archive")
				return err
			}
		} else {
			files.Context.Fields(log.Fields{"archivePath": files.BinArchive}).WithError(err).Error("can't archive bin file")
			return err
		}
	} else {
		files.Context.Fields(log.Fields{"archivePath": files.BinArchive}).Debug("Old bin archived")
	}
	return nil
}

func (files Files) rollback() error {
	var err error
	err = nil
	if err := files.Renamer(files.ConfigArchive, files.Config); err != nil {
		files.Context.Fields(log.Fields{"archived config":files.ConfigArchive, "used config":files.Config}).WithError(err).Error("can't rename config archive to used config path")
		return err
	}

	if originBinArchived, err := files.ReadLinker(files.BinArchive); err != nil {
		files.Context.Fields(log.Fields{"archived config":files.BinArchive}).WithError(err).Error("can't read origin of link to bin archive")
		return err
	} else {
		if err = files.Remover(files.Bin); err != nil {
			files.Context.Fields(log.Fields{"archived config":files.Bin}).WithError(err).Error("can't remove current link to bin")
			return err
		} else {
			if err = files.Linker(originBinArchived, files.Bin); err != nil {
				files.Context.Fields(log.Fields{"origin of link to binary":originBinArchived, "destination of link to binary":files.Bin}).WithError(err).Error("can't link binary to bin")
				return err
			} else {
				// success
				files.Context.Fields(log.Fields{"origin of link to binary":originBinArchived, "destination of link to binary":files.Bin}).Debug("rollback of link to haproxy binary")
			}
		}
	}

	return err
}

func (files Files) removeAll() error {
	files.Remover(files.ConfigArchive)
	files.Remover(files.BinArchive)
	if err := files.Remover(files.Config); err != nil {
		return err
	}
	return files.Remover(files.Bin)
}

func (files Files) configArchiveExists() bool {
	return files.Checker(files.ConfigArchive)
}

func osExists(path string) bool {
	if _, err := os.Stat(path); err == nil {
		return true
	}
	return false
}
