package com.vsct.dt.strowgr.admin.repository.consul;

import com.vsct.dt.strowgr.admin.core.EntryPointKeyDefaultImpl;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Created by william_montaz on 08/02/2016.
 */
@State(Scope.Benchmark)
public class ConsulRepositoryBenchmark {

    ConsulRepository repository = new ConsulRepository("localhost", 8500, 32000, 64000);

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ConsulRepositoryBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(1)
                .threads(1)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void acquire_release() {
        repository.lock(new EntryPointKeyDefaultImpl("some_key"));
        repository.release(new EntryPointKeyDefaultImpl("some_key"));
    }

}
