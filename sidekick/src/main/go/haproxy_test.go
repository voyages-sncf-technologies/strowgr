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
	"syscall"
	"testing"
)

func TestReloadScript(t *testing.T) {
	// given
	initContext()
	hap := newMockHaproxy()

	// test
	err := hap.reload("my_id")

	// check
	checkError(t, err)
	check(t, "reload command is wrong. actual: '%s', expected: '%s'", context.Command, "/HOME/TST/scripts/hapTSTDEV -f /HOME/TST/Config/hapTSTDEV.conf -p /HOME/TST/DEV/logs/haproxy.pid -sf 1234")
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
	binActual, contains := context.Links[newVersionExpected]
	check(t, "link to bin should be updated. actual %s, expected %s", contains, true)
	check(t, "link origin to new bin is wrong. actual %s, expected %s", binActual, binExpected)
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
	checkContains(t, "/HOME/TST/Config/hapTSTDEV.conf", context.Removed)
	checkContains(t, "/HOME/TST/scripts/hapTSTDEV", context.Removed)
	checkContains(t, "/HOME/TST/DEV", context.Removed)
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
	hap.Config.HapVersions = []string{"1", "2", "3"}
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
	hap := newMockHaproxy()
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
	hap := newMockHaproxy()

	binExpected := "/export/product/haproxy/product/1/bin/haproxy"
	newVersionExpected := "/HOME/TST/scripts/hapTSTDEV"

	conf := Conf{Version: "1"}
	conf.Haproxy = []byte("new conf")
	event := &EventMessageWithConf{Conf: conf}

	// test
	result, err := hap.ApplyConfiguration(event)

	// check
	checkError(t, err)
	check(t, "expected result is %s but actually got %s", SUCCESS, result)
	// check archive configuration
	checkMap(t, "/HOME/TST/version-1/hapTSTDEV.conf", "/HOME/TST/Config/hapTSTDEV.conf", context.Renames)
	// check archive bin
	actualArchivedVersion, contains := context.Links["/HOME/TST/version-1/hapTSTDEV"]
	check(t, "actual %s, expected %s", contains, true)
	check(t, "actual archive link %s but expected is %s", actualArchivedVersion, "/export/product/haproxy/product/1/bin/haproxy")

	//check(t, "expected archive link %s but actually got %s", "/export/product/haproxy/product/1/bin/haproxy", context.Bin)
	//check(t, "expected archive link %s but actually got %s", "/HOME/TST/version-1/hapTSTDEV", context.NewVersion)
	// check executions
	check(t, "reload command should be executed. expected command is '%s' but got '%s'", "/HOME/TST/scripts/hapTSTDEV -f /HOME/TST/Config/hapTSTDEV.conf -p /HOME/TST/DEV/logs/haproxy.pid -sf 1234", context.Command)
	// check links
	actualVersion, contains := context.Links[newVersionExpected]
	check(t, "link destination to new bin is wrong. actual %s, expected %s", contains, true)
	check(t, "link origin to new bin is wrong. actual %s, expected %s", actualVersion, binExpected)
	// check new conf
	check(t, "configuration file is %s but should be %s ", context.Writes["/HOME/TST/Config/hapTSTDEV.conf"], "new conf")
}

func TestEmptyConfiguration(t *testing.T) {
	// given
	initContext()
	hap := newMockHaproxy()
	hap.Files.Reader = MockReaderEmpty
	hap.Files.Checker = MockCheckerAbsent

	binExpected := "/export/product/haproxy/product/1/bin/haproxy"
	newVersionExpected := "/HOME/TST/scripts/hapTSTDEV"

	conf := Conf{Version: "1"}
	conf.Haproxy = []byte("completly new conf")
	event := &EventMessageWithConf{Conf: conf}

	// test
	result, err := hap.ApplyConfiguration(event)

	// check
	checkError(t, err)
	check(t, "expected result is %s but actually got %s", SUCCESS, result)
	// check executions
	check(t, "reload command should be executed. expected command is '%s' but got '%s'", "/HOME/TST/scripts/hapTSTDEV -f /HOME/TST/Config/hapTSTDEV.conf -p /HOME/TST/DEV/logs/haproxy.pid", context.Command)
	// check links
	actualVersion, contains := context.Links[newVersionExpected]
	check(t, "link destination to new bin is wrong. actual %s, expected %s", contains, true)
	check(t, "link origin to new bin is wrong. actual %s, expected %s", actualVersion, binExpected)
	// check new conf
	check(t, "configuration file is %s but should be %s ", context.Writes["/HOME/TST/Config/hapTSTDEV.conf"], "completly new conf")
}


