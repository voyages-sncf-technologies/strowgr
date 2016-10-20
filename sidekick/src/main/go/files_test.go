package sidekick

import "testing"

func TestArchive(t *testing.T) {
	initContext()
	// given
	files := &Files{
		Renamer:MockRenamer,
		Remover:MockRemover,
		ReadLinker:MockReadLinker,
		Checker: MockChecker,
		Linker:MockLinker,
		Config: "/path/to/haproxy/config",
		ConfigArchive: "/path/to/haproxy/archived/config",
		BinArchive:"/path/to/haproxy/archived/bin",
	}

	// test
	err := files.archive()

	// check
	checkError(t, err)
	checkMap(t, files.ConfigArchive, files.Config, context.Links)
	checkMap(t, files.BinArchive, "/export/product/haproxy/product/1/bin/haproxy", context.Links
	// TODO test remove
}

func TestRollback(t *testing.T) {
	initContext()
	// given
	files := &Files{
		Renamer:MockRenamer,
		Remover:MockRemover,
		ReadLinker:MockReadLinker,
		Checker: MockChecker,
		Linker:MockLinker,
		Config: "/path/to/haproxy/config",
		ConfigArchive: "/path/to/haproxy/archived/config",
		BinArchive:"/path/to/haproxy/archived/bin",
		Bin:"/path/to/haproxy/bin",
	}

	// test
	err := files.rollback()

	// check
	checkError(t, err)
	checkMap(t, files.Config, files.ConfigArchive, context.Renames)
	checkMap(t, files.Bin, "/export/product/haproxy/product/2/bin/haproxy", context.Links)
}