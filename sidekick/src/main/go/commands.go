package sidekick

import (
	"os"
	"io/ioutil"
)


// commands
type Commands interface {
	Reader(path string) ([]byte, error)
	Writer(path string, content []byte, perm os.FileMode) error
	Renamer(oldpath, newpath string) error
	Exists(path string) bool
	Linker(oldpath, newpath string) error
	Remover(path string, isPanic bool) error
	ReadLinker(path string) (string, error)
	MkdirAll(path string) error
}


// os implementation
type OsCommands struct {

}

func (osCmd OsCommands) Reader(path string) ([]byte, error) {
	return ioutil.ReadFile(path)
}

func (osCmd OsCommands) Writer(path string, content []byte, perm os.FileMode) error {
	return ioutil.WriteFile(path, content, perm)
}

func (osCmd OsCommands) Renamer(oldpath, newpath string) error {
	return os.Rename(oldpath, newpath)
}

func (osCmd OsCommands) Exists(path string) bool {
	if _, err := os.Stat(path); err == nil {
		return true
	}
	return false
}

func (osCmd OsCommands) Linker(oldpath, newpath string) error {
	if err := os.Symlink(oldpath, newpath); os.IsExist(err) {
		if err = osCmd.Remover(newpath, false); err != nil {
			return err
		} else {
			return osCmd.Linker(oldpath, newpath)
		}
	} else {
		return err
	}
}

func (osCmd OsCommands) Remover(path string, isPanic bool) (err error) {
	if isPanic {
		if err := os.Remove(path); err != nil {
			panic(err)
		}
	} else {
		return os.Remove(path)
	}
	return nil
}

func (osCmd OsCommands) ReadLinker(path string) (string, error) {
	return os.Readlink(path)
}

func (osCmd OsCommands) MkdirAll(path string) error {
	if !osCmd.Exists(path) {
		if err := os.MkdirAll(path, 0755); err != nil {
			panic(err)
		}
	}
	return nil
}
