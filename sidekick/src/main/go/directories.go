package sidekick

import (
	"os"
	log "github.com/Sirupsen/logrus"
)

type Directories struct {
	Map     map[string]string
	Mkdir   Mkdir
	Context Context
	Remover Remover
}

type Mkdir func(context Context, directory string) error

func NewDirectories(context Context, directories map[string]string) Directories {
	return Directories{
		Map:directories,
		Context:context,
		Mkdir:createDirectory,
		Remover: os.Remove,
	}
}

// createSkeleton creates the directory tree for a new haproxy context
func (directories *Directories) mkDirs(context Context) error {
	for _, directory := range directories.Map {
		err := directories.Mkdir(context, directory)
		if err != nil {
			return err
		}
	}
	return nil
}

// createDirectory recursively creates directory if it doesn't exists
func createDirectory(context Context, dir string) error {
	if _, err := os.Stat(dir); os.IsNotExist(err) {
		err := os.MkdirAll(dir, 0755)
		if err != nil {
			context.Fields(log.Fields{"dir": dir}).WithError(err).Error("Failed to create")
			return err
		} else {
			context.Fields(log.Fields{"dir": dir}).WithError(err).Info("Directory created")
		}
	}
	return nil
}

func (directories Directories) removePlatform() error {
	return directories.Remover(directories.Map["Platform"])
}