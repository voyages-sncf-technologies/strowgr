package sidekick

import (
	"io/ioutil"
	"os"
)

// commands
type Commands interface {
	Reader(path string, isPanic bool) ([]byte, error)
	Writer(path string, content []byte, perm os.FileMode, isPanic bool) error
	Renamer(oldpath, newpath string, isPanic bool) error
	Exists(path string) bool
	Linker(oldpath, newpath string, isPanic bool) error
	Remover(path string, isPanic bool) error
	ReadLinker(path string, isPanic bool) (string, error)
	MkdirAll(path string) error
}

// os implementation
type OsCommands struct {
}

func (osCmd OsCommands) Reader(path string, isPanic bool) ([]byte, error) {
	result, err := ioutil.ReadFile(path)
	if err != nil && isPanic {
		panic(err)
	}
	return result, err
}

func (osCmd OsCommands) Writer(path string, content []byte, perm os.FileMode, isPanic bool) error {
	err := ioutil.WriteFile(path, content, perm)
	if err != nil && isPanic {
		panic(err)
	}
	return err
}

func (osCmd OsCommands) Renamer(oldpath, newpath string, isPanic bool) error {
	err := os.Rename(oldpath, newpath)
	if err != nil && isPanic {
		panic(err)
	}
	return err
}

func (osCmd OsCommands) Exists(path string) bool {
	if _, err := os.Stat(path); err == nil {
		return true
	}
	return false
}

func (osCmd OsCommands) Linker(oldpath, newpath string, isPanic bool) error {
	if err := os.Symlink(oldpath, newpath); os.IsExist(err) {
		if err = osCmd.Remover(newpath, false); err != nil {
			if isPanic {
				panic(err)
			} else {
				return err
			}
		} else {
			return osCmd.Linker(oldpath, newpath, isPanic)
		}
	} else {
		return err
	}
}

func (osCmd OsCommands) Remover(path string, isPanic bool) (err error) {
	if isPanic {
		if err := os.RemoveAll(path); err != nil {
			panic(err)
		}
	} else {
		return os.Remove(path)
	}
	return nil
}

func (osCmd OsCommands) ReadLinker(path string, isPanic bool) (string, error) {
	if result, err := os.Readlink(path); err != nil && isPanic {
		panic(err)
	} else {
		return result, err
	}
}

func (osCmd OsCommands) MkdirAll(path string) error {
	if !osCmd.Exists(path) {
		if err := os.MkdirAll(path, 0755); err != nil {
			panic(err)
		}
	}
	return nil
}
