package sidekick

import (
	"errors"
	"os"
	"strings"
	"testing"
)

/////////////////
// Test tools  //
/////////////////

type TestContext struct {
	Links   map[string]string
	Renames map[string]string
	Removed []string
	Command string
	Signal  os.Signal
	Writes  map[string]string
}

var context TestContext

func initContext() {
	context = TestContext{Removed: []string{}, Command: "", Writes: make(map[string]string), Renames: make(map[string]string), Links: make(map[string]string)}
}

type MockCommands struct{}

func (mc MockCommands) Writer(path string, content []byte, perm os.FileMode, isPanic bool) error {
	context.Writes[path] = string(content)
	return nil
}

func (mc MockCommands) Renamer(oldPath, newPath string, isPanic bool) error {
	context.Renames[newPath] = oldPath
	return nil
}

func MockCommand(name string, arg ...string) ([]byte, error) {
	context.Command = name + " " + strings.Join(arg, " ")
	return []byte("ok"), nil
}

func MockFailedCommand(name string, arg ...string) ([]byte, error) {
	return nil, errors.New("fails...")
}

func (mc MockCommands) Reader(path string, isPanic bool) ([]byte, error) {
	if strings.HasSuffix(path, "pid") {
		return []byte("1234"), nil
	} else if strings.HasSuffix(path, "conf") {
		return []byte("my conf"), nil
	} else if strings.HasSuffix(path, "VERSION") {
		return []byte("1"), nil
	} else {
		return []byte("nothing"), nil
	}
}

func (mc MockCommands) ReaderEmpty(path string) ([]byte, error) {
	if strings.HasSuffix(path, "pid") {
		return nil, errors.New("pid is not present")
	} else if strings.HasSuffix(path, "conf") {
		return nil, errors.New("conf is not present")
	} else {
		return []byte("nothing"), nil
	}
}

func (mc MockCommands) Linker(origin, destination string, isPanic bool) error {
	context.Links[destination] = origin
	return nil
}

func (mc MockCommands) Exists(newVersion string) bool {
	return true
}

func (mc MockCommands) CheckerAbsent(newVersion string, isPanic bool) bool {
	return false
}

func (mc MockCommands) Remover(path string, isPanic bool) error {
	if strings.Contains(path, "version") {
		if isPanic {
			panic("archive file " + path + " is not present")
		}
		return errors.New("archive file " + path + " is not present")
	}
	context.Removed = append(context.Removed, path)
	return nil
}

func MockSignal(pid int, signal os.Signal) error {
	context.Signal = signal
	return nil
}

func (mc MockCommands) MkdirAll(directory string) error {
	return nil
}

func (mc MockCommands) ReadLinker(link string, isPanic bool) (string, error) {
	if strings.Contains(link, "archived") {
		return "/export/product/haproxy/product/2/bin/haproxy", nil
	}
	return "/export/product/haproxy/product/1/bin/haproxy", nil
}

func newMockHaproxy() *Haproxy {
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})
	hap.Filesystem.Commands = MockCommands{}
	hap.Command = MockCommand
	hap.Config.Hap = createHapInstallations()
	return hap
}

func createHapInstallations() map[string]HapInstallation {
	versions := make(map[string]HapInstallation)
	versions["1"] = HapInstallation{
		Path: "/export/product/haproxy/product/1",
	}
	versions["2"] = HapInstallation{
		Path: "/export/product/haproxy/product/2",
	}
	versions["3"] = HapInstallation{
		Path: "/export/product/haproxy/product/3",
	}
	return versions
}

func check(t *testing.T, message string, actual, expected interface{}) {
	if actual != expected {
		t.Errorf(message, actual, expected)
		t.Fail()
	}
}

func checkMap(t *testing.T, expectedKey string, expectedValue string, data map[string]string) {
	actualValue, contains := data[expectedKey]
	if contains {
		if actualValue == expectedValue {
			//
		} else {
			t.Errorf("should contains value %s for key %s, but value is %s", expectedValue, expectedKey, actualValue)
			t.Fail()
		}
	}
}

func checkContains(t *testing.T, expectedValue string, data []string) {
	contains := false
	for _, v := range data {
		contains = contains || (v == expectedValue)
	}
	if !contains {
		t.Errorf("expected value %s is not present in %s", expectedValue, data)
		t.Fail()
	}
}

func checkError(t *testing.T, err error) {
	if err != nil {
		t.Errorf("unexpected error: %s", err)
		t.Fail()
	}
}
