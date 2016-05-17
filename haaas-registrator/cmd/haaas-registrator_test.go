package main

import (
	"testing"
	"./.."
	"github.com/docker/engine-api/types"
	"github.com/docker/engine-api/types/container"
	"reflect"
	"runtime"
)

func instanceFixture() *haaasregistrator.Instance {
	instance := haaasregistrator.NewInstance()
	instance.App = "Test"
	instance.Platform = "TST"
	instance.Service = "BACK"
	instance.Port = "1234"
	instance.Ip = "1.2.3.4"

	return instance
}

func containerJSONFixture() types.ContainerJSON {
	return types.ContainerJSON{
		ContainerJSONBase : &types.ContainerJSONBase{
			Name: "test",
		},
	}
}

func TestDefaultNamingStrategy(t *testing.T) {

	strategyFunc := defaultNamingStrategy

	expected := "1_2_3_4_test_1234"
	result := strategyFunc(containerJSONFixture(), instanceFixture())
	AssertEquals(t, expected, result)
}

func TestNamingStrategy(t *testing.T) {

	strategyFunc := defaultNamingStrategy

	expected := "1_2_3_4_test_1234"
	result := strategyFunc(containerJSONFixture(), instanceFixture())
	AssertEquals(t, expected, result)
}

func TestContainerNamingStrategySelector_default(t *testing.T) {
	var config = &container.Config{
		Labels:map[string]string{
			"registrator.id_generator" : "pouet",
		},
	}

	result := getNamingStrategy(config)
	expected := defaultNamingStrategy
	AssertEquals(t, GetFunctionName(expected), GetFunctionName(result))
}

func TestContainerNamingStrategySelector_container(t *testing.T) {
	var config = &container.Config{
		Labels:map[string]string{
			"registrator.id_generator" : "container_name",
		},
	}

	result := getNamingStrategy(config)
	expected := containerNamingStrategy
	AssertEquals(t, GetFunctionName(expected), GetFunctionName(result))
}

func AssertEquals(t *testing.T, expected interface{}, result interface{}) {
	if result != expected {
		t.Logf("Expected '%s', got '%s'", expected, result)
		t.Fail()
	}
}

func GetFunctionName(i interface{}) string {
	return runtime.FuncForPC(reflect.ValueOf(i).Pointer()).Name()
}