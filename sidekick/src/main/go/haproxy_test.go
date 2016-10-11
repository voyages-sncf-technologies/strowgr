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
	"io/ioutil"
	"os"
	"runtime"
	"testing"
	"fmt"
)

func mock_command(name string, arg ...string) ([]byte, error) {
	return []byte("output"), nil
}

var (
	config = Config{HapHome: "/HOME"}

)

func TestGetReloadScript(t *testing.T) {
	hap := NewHaproxy(&Config{HapHome: "/HOME"},Context{Application: "TST", Platform: "DEV"})
	config.HapHome = "/HOME"
	result := hap.getReloadScript()
	expected := "/HOME/TST/scripts/hapTSTDEV"
	AssertEquals(t, expected, result)
}

func TestReloadScript(t *testing.T) {
	// given
	tmpDir := os.TempDir()
	hap := NewHaproxy(&Config{HapHome: tmpDir},Context{Application: "TST", Platform: "DEV"})
	hap.Command = mock_command
	err := os.MkdirAll(tmpDir + "/TST/logs/TSTDEV/", 0644)
	if err != nil {
		t.Fail()
	}
	err = ioutil.WriteFile(tmpDir + "/TST/logs/TSTDEV/haproxy.pid", []byte("1234"), 0644)
	if err != nil {
		t.Fail()
	}
	config.HapHome = tmpDir

	// test
	fmt.Println(hap.Paths.Pid)
	err = hap.reload("my_id")

	// check
	if err != nil {
		t.Fail()
	}
}

func TestCreateSkeleton(t *testing.T) {
	// given
	tmpdir, _ := ioutil.TempDir("", "strowgr")
	defer os.Remove(tmpdir)
	hap := NewHaproxy(&Config{HapHome: tmpdir},Context{Application: "TST", Platform: "DEV"})


	// test
	hap.createSkeleton("mycorrelationid")

	// check
	AssertFileExists(t, tmpdir + "/TST/Config")
	AssertFileExists(t, tmpdir + "/TST/logs/TSTDEV")
	AssertFileExists(t, tmpdir + "/TST/scripts")
	AssertFileExists(t, tmpdir + "/TST/version-1")
}

func TestHapBinLink(t *testing.T) {
	// given
	tmpdir, _ := ioutil.TempDir("", "strowgr")
	defer os.Remove(tmpdir)
	hap := NewHaproxy(&Config{HapHome: tmpdir},Context{Application: "TST", Platform: "DEV"})
	hap.createSkeleton("mycorrelationid")

	// test
	err := updateSymlink(hap.Context, "mycorrelationid", tmpdir, hap.HaproxyBinLink)

	// check
	if err != nil {
		t.Error(err)
	}
	if runtime.GOOS != "windows" {
		AssertIsSymlink(t, tmpdir + "/TST/scripts/hapTSTDEV")
	}
}

func TestArchivePath(t *testing.T) {
	hap := NewHaproxy(&Config{HapHome: "/HOME"},Context{Application: "TST", Platform: "DEV"})

	result := hap.Paths.Archive
	expected := "/HOME/TST/version-1/hapTSTDEV.conf"
	AssertEquals(t, expected, result)
}

func AssertFileExists(t *testing.T, file string) {
	if _, err := os.Stat(file); os.IsNotExist(err) {
		t.Logf("File or directory '%s' does not exists", file)
		t.Fail()
	}
}

func AssertFileNotExists(t *testing.T, file string) {
	if _, err := os.Stat(file); os.IsExist(err) {
		t.Logf("File or directory '%s' exists", file)
		t.Fail()
	}
}

func AssertIsSymlink(t *testing.T, file string) {
	fi, err := os.Lstat(file)
	if err != nil || (fi.Mode() & os.ModeSymlink != os.ModeSymlink) {
		t.Logf("File or directory '%s' does not exists", file)
		t.Fail()
	}
}

func AssertEquals(t *testing.T, expected interface{}, result interface{}) {
	if result != expected {
		t.Logf("Expected '%s', got '%s'", expected, result)
		t.Fail()
	}
}

func TestDeleteInstance(t *testing.T) {
	tmpdir, _ := ioutil.TempDir("", "strowgr")
	defer os.Remove(tmpdir)
	hap := NewHaproxy(&Config{HapHome: tmpdir},Context{Application: "TST", Platform: "DEV"})

	err := hap.createSkeleton("mycorrelationid")

	if err != nil {
		t.Error(err)
		t.Fail()
	}
	AssertFileExists(t, tmpdir + "/TST/Config")
	hap.Delete()

	AssertFileNotExists(t, tmpdir + "/TST")
	AssertFileExists(t, tmpdir)
}
