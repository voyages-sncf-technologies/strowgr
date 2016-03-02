package haaasd
import (
	"testing"
	"io/ioutil"
	"os"
	"runtime"
)

var (
	config = Config{HapHome: "/HOME", }
	hap = NewHaproxy(&config, "TST", "DEV", "1.4.22")
)

func TestGetReloadScript(t *testing.T) {
	config.HapHome = "/HOME"
	result := hap.getReloadScript()
	expected := "/HOME/TST/scripts/hapctlTSTDEV"
	AssertEquals(t,expected,result)
}


func TestCreateSkeleton(t *testing.T) {
	tmpdir, _ := ioutil.TempDir("", "haaas")
	defer os.Remove(tmpdir)
	config.HapHome = tmpdir
	hap.createSkeleton()
	AssertFileExists(t, tmpdir + "/TST/Config")
	AssertFileExists(t, tmpdir + "/TST/logs/TSTDEV")
	AssertFileExists(t, tmpdir + "/TST/scripts")
	AssertFileExists(t, tmpdir + "/TST/version-1")
	if runtime.GOOS != "windows" {
		AssertFileExists(t, tmpdir + "/TST/Config/haproxy")
		AssertFileExists(t, tmpdir + "/TST/scripts/hapctlTSTDEV")
	}
}

func TestArchivePath(t *testing.T) {
	config.HapHome = "/HOME"
	result := hap.confArchivePath()
	expected := "/HOME/TST/version-1/hapTSTDEV.conf"
	AssertEquals(t,expected,result)
}

func AssertFileExists(t *testing.T, file string) {
	if _, err := os.Stat(file); os.IsNotExist(err) {
		t.Logf("File or directory '%s' does not exists", file)
		t.Fail()
	}
}

func AssertEquals(t *testing.T, expected interface{},result interface{}) {
	if result != expected {
		t.Logf("Expected '%s', got '%s'", expected, result)
		t.Fail()
	}
}
