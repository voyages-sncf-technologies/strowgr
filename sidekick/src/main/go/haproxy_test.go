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
)

var (
	config = Config{HapHome: "/HOME"}
	hap    = NewHaproxy("master", &config, Context{Application: "TST", Platform: "DEV"})
)

func TestGetReloadScript(t *testing.T) {
	config.HapHome = "/HOME"
	result := hap.getReloadScript()
	expected := "/HOME/TST/scripts/hapctlTSTDEV"
	AssertEquals(t, expected, result)
}

func TestCreateSkeleton(t *testing.T) {
	tmpdir, _ := ioutil.TempDir("", "strowgr")
	defer os.Remove(tmpdir)
	config.HapHome = tmpdir
	hap.createSkeleton("mycorrelationid")
	AssertFileExists(t, tmpdir+"/TST/Config")
	AssertFileExists(t, tmpdir+"/TST/logs/TSTDEV")
	AssertFileExists(t, tmpdir+"/TST/scripts")
	AssertFileExists(t, tmpdir+"/TST/version-1")
	if runtime.GOOS != "windows" {
		AssertIsSymlink(t, tmpdir+"/TST/Config/haproxy")
		AssertIsSymlink(t, tmpdir+"/TST/scripts/hapctlTSTDEV")
	}
}

func TestArchivePath(t *testing.T) {
	config.HapHome = "/HOME"
	result := hap.confArchivePath()
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
	if err != nil || (fi.Mode()&os.ModeSymlink != os.ModeSymlink) {
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
	config.HapHome = tmpdir
	hap.createSkeleton("mycorrelationid")
	AssertFileExists(t, tmpdir+"/TST/Config")
	hap.Delete()

	AssertFileNotExists(t, tmpdir+"/TST")
	AssertFileExists(t, tmpdir)
}
