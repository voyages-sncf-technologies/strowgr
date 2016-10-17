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
	"os"
	"strings"
	"syscall"
	"testing"
)

var (
	config = Config{HapHome: "/HOME"}
)

func TestReloadScript(t *testing.T) {
	// given
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})
	hap.Command = MockCommand
	hap.Files.Reader = MockReader

	// test
	err := hap.reload("my_id")

	// check
	checkError(t, err)
	check(t, "reload command is wrong. actual: '%s', expected: '%s'", context.Command, "/HOME/TST/scripts/hapTSTDEV -f /HOME/TST/Config/hapTSTDEV.conf -p /HOME/TST/logs/TSTDEV/haproxy.pid -sf 1234")
}

func TestCreateDirectory(t *testing.T) {
	// given & test
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})

	// check
	check(t, "actual directory path: %s, expected: %s", hap.Directories.Map["Config"], "/HOME/TST/Config")
	check(t, "actual directory path: %s, expected: %s", hap.Directories.Map["Logs"], "/HOME/TST/DEV/logs")
	check(t, "actual directory path: %s, expected: %s", hap.Directories.Map["Scripts"], "/HOME/TST/scripts")
	check(t, "actual directory path: %s, expected: %s", hap.Directories.Map["VersionMinus1"], "/HOME/TST/version-1")
	check(t, "actual directory path: %s, expected: %s", hap.Directories.Map["Errors"], "/HOME/TST/DEV/errors")
	check(t, "actual directory path: %s, expected: %s", hap.Directories.Map["Dump"], "/HOME/TST/DEV/dump")
	check(t, "actual directory path: %s, expected: %s", hap.Directories.Map["Syslog"], "/HOME/SYSLOG/Config/syslog.conf.d")
}

func TestLinkNewVersion(t *testing.T) {
	// given
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})
	hap.Files.Linker = MockLinker
	hap.Files.Checker = MockChecker
	hap.Files.Remover = MockRemover
	binExpected := "/export/product/haproxy/product/1.2.3/bin/haproxy"
	newVersionExpected := "/HOME/TST/scripts/hapTSTDEV"

	// test
	err := hap.Files.linkNewVersion("1.2.3")

	// check
	checkError(t, err)
	check(t, "link destination to new bin is wrong. actual %s, expected %s", context.NewVersion, newVersionExpected)
	check(t, "link origin to new bin is wrong. actual %s, expected %s", context.Bin, binExpected)
	check(t, "link is not removed as expected. actual %, expected %s", context.Removed, true)
	check(t, "old link is not removed as expected. actual %, expected %s", context.RemovedPath, ";"+newVersionExpected)
}

func TestArchivePath(t *testing.T) {
	// given
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})

	// test & check
	result := hap.Files.ConfigArchive
	expected := "/HOME/TST/version-1/hapTSTDEV.conf"
	check(t, "Expected '%s', got '%s'", expected, result)
}

func TestDeleteInstance(t *testing.T) {
	// given
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})
	hap.Directories.Remover = MockRemover
	hap.Files.Remover = MockRemover

	// test
	hap.Delete()

	// check
	check(t, "Actually removed files: %s. One of these files is not removed: %s. ", context.RemovedPath, ";/HOME/TST/Config/hapTSTDEV.conf;/HOME/TST/scripts/hapTSTDEV;/HOME/TST/DEV")
}

func TestStop(t *testing.T) {
	// given
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})
	hap.Signal = MockSignal
	hap.Files.Reader = MockReader

	// test
	err := hap.Stop()

	// check
	checkError(t, err)
	check(t, "expected signal sent to haproxy process is %s but should be %s", context.Signal, syscall.SIGTERM)
}

func TestUnversionedConfiguration(t *testing.T) {
	// given
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})
	hap.properties.HapVersions = []string{"1", "2", "3"}
	hap.Files.Reader = MockReader
	conf := Conf{Version: "123"}
	event := &EventMessageWithConf{Conf: conf}

	// test
	result, _ := hap.ApplyConfiguration(event)

	// given
	check(t, "expected result is %s but actually got %s", ERR_CONF, result)
}

func TestUnchangedConfiguration(t *testing.T) {
	// given
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})
	hap.properties.HapVersions = []string{"1", "2", "3"}
	hap.Files.Reader = MockReader
	hap.Files.Renamer = MockRenamer
	hap.Files.Linker = MockLinker
	hap.Command = MockCommand
	conf := Conf{Version: "1"}
	conf.Haproxy = []byte("my conf")
	event := &EventMessageWithConf{Conf: conf}

	// test
	result, err := hap.ApplyConfiguration(event)

	// given
	checkError(t, err)
	check(t, "expected result is %s but actually got %s", UNCHANGED, result)
	check(t, "actual command %s should be empty but got %s", "", context.Command)
}

func TestChangedConfiguration(t *testing.T) {
	// given
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})
	hap.properties.HapVersions = []string{"1", "2", "3"}
	hap.Files.Reader = MockReader
	hap.Files.Writer = MockWriter
	hap.Files.Renamer = MockRenamer
	hap.Files.Checker = MockChecker

	hap.Command = MockCommand
	hap.Directories.Mkdir = MockMkdir
	conf := Conf{Version: "1"}
	conf.Haproxy = []byte("new conf")
	event := &EventMessageWithConf{Conf: conf}

	// test
	result, err := hap.ApplyConfiguration(event)

	// given
	checkError(t, err)
	check(t, "expected result is %s but actually got %s", SUCCESS, result)
	check(t, "expected configuration to archive %s but actually got %s", "/HOME/TST/Config/hapTSTDEV.conf", context.OldPaths[0])
	check(t, "expected archive path %s but actually got %s", "/HOME/TST/version-1/hapTSTDEV.conf", context.NewPaths[0])
	check(t, "expected archive link %s but actually got %s", "/HOME/TST/scripts/hapTSTDEV", context.OldPaths[1])
	check(t, "expected archive link %s but actually got %s", "/HOME/TST/version-1/hapTSTDEV", context.NewPaths[1])
}

/////////////////
// Test tools  //
/////////////////

type TestContext struct {
	NewVersion  string
	Bin         string
	RemovedPath string
	Removed     bool
	Command     string
	OldPaths    []string
	NewPaths    []string
	Signal      os.Signal
}

var context TestContext

func initContext() {
	context = TestContext{NewVersion: "", Bin: "", Removed: false, RemovedPath: "", Command: ""}
}

func MockRenamer(oldPath, newPath string) error {
	context.OldPaths = append(context.OldPaths, oldPath)
	context.NewPaths = append(context.NewPaths, newPath)
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

func MockLinker(bin, newVersion string) error {
	context.Bin = bin
	context.NewVersion = newVersion
	return nil
}

func MockChecker(newVersion string) bool {
	return true
}

func MockRemover(path string) error {
	context.Removed = true
	context.RemovedPath += ";" + path
	return nil
}

func MockSignal(pid int, signal os.Signal) error {
	context.Signal = signal
	return nil
}

func MockMkdir(Context Context, directory string) error {
	return nil
}

func MockWriter(path string, content []byte, perm os.FileMode) error {
	return nil
}
func check(t *testing.T, message string, actual, expected interface{}) {
	if actual != expected {
		t.Errorf(message, actual, expected)
		t.Fail()
	}
}

func checkError(t *testing.T, err error) {
	if err != nil {
		t.Errorf("unexpected error: %s", err)
		t.Fail()
	}
}
