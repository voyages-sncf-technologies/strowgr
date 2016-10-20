package sidekick

import (
	"os"
	"testing"
	"strings"
	"errors"
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
	context = TestContext{Removed: []string{}, Command: "", Writes:make(map[string]string), Renames:make(map[string]string), Links:make(map[string]string)}
}

func MockWriter(path string, content []byte, perm os.FileMode) error {
	context.Writes[path] = string(content)
	return nil
}

func MockRenamer(oldPath, newPath string) error {
	context.Renames[newPath] = oldPath
	return nil
}

func MockCommand(name string, arg ...string) ([]byte, error) {
	context.Command = name + " " + strings.Join(arg, " ")
	return []byte("ok"), nil
}

func MockReader(path string) ([]byte, error) {
	if strings.HasSuffix(path, "pid") {
		return []byte("1234"), nil
	} else if strings.HasSuffix(path, "conf") {
		return []byte("my conf"), nil
	} else {
		return []byte("nothing"), nil
	}
}

func MockReaderEmpty(path string) ([]byte, error) {
	if strings.HasSuffix(path, "pid") {
		return nil, errors.New("pid is not present")
	} else if strings.HasSuffix(path, "conf") {
		return nil, errors.New("conf is not present")
	} else {
		return []byte("nothing"), nil
	}
}

func MockLinker(origin, destination string) error {
	context.Links[destination] = origin
	return nil
}

func MockChecker(newVersion string) bool {
	return true
}

func MockCheckerAbsent(newVersion string) bool {
	return false
}

func MockRemover(path string) error {
	context.Removed = append(context.Removed, path)
	return nil
}

func MockSignal(pid int, signal os.Signal) error {
	context.Signal = signal
	return nil
}

func MockMkdir(Context Context, directory string) error {
	return nil
}

func MockReadLinker(link string) (string, error) {
	if strings.Contains(link, "archived") {
		return "/export/product/haproxy/product/2/bin/haproxy", nil
	}
	return "/export/product/haproxy/product/1/bin/haproxy", nil
}

func newMockHaproxy() *Haproxy {
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})
	hap.Config.HapVersions = []string{"1", "2", "3"}
	hap.Files.Reader = MockReader
	hap.Files.Writer = MockWriter
	hap.Files.Renamer = MockRenamer
	hap.Files.Checker = MockChecker
	hap.Files.Linker = MockLinker
	hap.Files.ReadLinker = MockReadLinker
	hap.Command = MockCommand
	hap.Directories.Mkdir = MockMkdir
	return hap
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
