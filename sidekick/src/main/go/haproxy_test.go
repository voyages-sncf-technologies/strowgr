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

func TestReLoadScript(t *testing.T) {
	// given
	initContext()
	hap := newMockHaproxy()

	// test
	err := hap.reload("my_id")

	// check
	checkError(t, err)
	check(t, "reload command is wrong. actual: '%s', expected: '%s'", context.Command, "/HOME/TST/scripts/hapTSTDEV -f /HOME/TST/Config/hapTSTDEV.conf -p /HOME/TST/logs/TSTDEV/haproxy.pid -sf 1234")
}

func TestReloadFails(t *testing.T) {
	// given
	initContext()
	hap := newMockHaproxy()
	hap.Command = MockFailedCommand

	//test
	err := hap.reload("my_id")

	// check
	check(t, "actual error '%s', error expected '%s'", err.Error(), "fails...")
}

func TestCreateDirectory(t *testing.T) {
	// given & test
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})

	// check
	check(t, "actual directory path: %s, expected: %s", hap.Filesystem.Application.Config, "/HOME/TST/Config")
	check(t, "actual directory path: %s, expected: %s", hap.Filesystem.Application.Scripts, "/HOME/TST/scripts")
	check(t, "actual directory path: %s, expected: %s", hap.Filesystem.Application.Archives, "/HOME/TST/version-1")
	check(t, "actual directory path: %s, expected: %s", hap.Filesystem.Platform.Logs, "/HOME/TST/DEV/logs")
	check(t, "actual directory path: %s, expected: %s", hap.Filesystem.Platform.Errors, "/HOME/TST/DEV/errors")
	check(t, "actual directory path: %s, expected: %s", hap.Filesystem.Platform.Dump, "/HOME/TST/DEV/dump")
	check(t, "actual directory path: %s, expected: %s", hap.Filesystem.Syslog.Path, "/HOME/SYSLOG/Config/syslog.conf.d")
}

func TestArchivePath(t *testing.T) {
	// given
	initContext()
	hap := NewHaproxy(&Config{HapHome: "/HOME"}, Context{Application: "TST", Platform: "DEV"})

	// test & check
	result := hap.Filesystem.Files.ConfigArchive
	expected := "/HOME/TST/version-1/hapTSTDEV.conf"
	check(t, "Expected '%s', got '%s'", expected, result)
}

func TestDeleteInstance(t *testing.T) {
	// given
	initContext()
	hap := newMockHaproxy()

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
	hap := newMockHaproxy()
	hap.Signal = MockSignal

	// test
	err := hap.Stop()

	// check
	checkError(t, err)
	check(t, "expected signal sent to haproxy process is %s but should be %s", context.Signal, syscall.SIGTERM)
}

func TestUnversionedConfiguration(t *testing.T) {
	// given
	initContext()
	hap := newMockHaproxy()
	hap.Config.HapVersions = []string{"1", "2", "3"}
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

	check(t, "expected archive link %s but actually got %s", "/export/product/haproxy/product/1/bin/haproxy", context.Links["/HOME/TST/version-1/hapTSTDEV"])
	// check executions
	check(t, "reload command should be executed. expected command is '%s' but got '%s'", "/HOME/TST/scripts/hapTSTDEV -f /HOME/TST/Config/hapTSTDEV.conf -p /HOME/TST/logs/TSTDEV/haproxy.pid -sf 1234", context.Command)
	// check links
	actualVersion, contains := context.Links[newVersionExpected]
	check(t, "link destination to new bin is wrong. actual %s, expected %s", contains, true)
	check(t, "link origin to new bin is wrong. actual '%s', expected '%s'", actualVersion, binExpected)
	// check new conf
	check(t, "configuration file is '%s' but should be '%s' ", context.Writes["/HOME/TST/Config/hapTSTDEV.conf"], "new conf")
}

func TestApplyConfigurationWithFailedReload(t *testing.T) {
	// given
	initContext()
	hap := newMockHaproxy()
	hap.Command = MockFailedCommand

	conf := Conf{Version: "1"}
	conf.Haproxy = []byte("new conf")
	event := &EventMessageWithConf{Conf: conf}

	// test
	result, err := hap.ApplyConfiguration(event)

	// check
	check(t, "actual error '%s', error expected '%s'", err.Error(), "fails...")
	check(t, "actual return status is '%s', ", result, ERR_RELOAD)

}

func TestEmptyConfiguration(t *testing.T) {
	// given
	initContext()
	hap := newMockHaproxy()

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
	check(t, "reload command should be executed. expected command is '%s' but got '%s'", "/HOME/TST/scripts/hapTSTDEV -f /HOME/TST/Config/hapTSTDEV.conf -p /HOME/TST/DEV/logs/haproxy.pid -sf 1234", context.Command)

	// check links
	actualVersion, contains := context.Links[newVersionExpected]
	check(t, "link destination to new bin is wrong. actual %s, expected %s", contains, true)
	check(t, "link origin to new bin is wrong. actual %s, expected %s", actualVersion, binExpected)
	// check new conf
	check(t, "configuration file is %s but should be %s ", context.Writes["/HOME/TST/Config/hapTSTDEV.conf"], "completly new conf")
}
