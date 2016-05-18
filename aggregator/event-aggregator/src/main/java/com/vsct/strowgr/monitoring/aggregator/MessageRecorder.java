package com.vsct.strowgr.monitoring.aggregator;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.vsct.strowgr.monitoring.aggregator.cassandra.ErrorRecord;
import com.vsct.strowgr.monitoring.aggregator.cassandra.ErrorRecordWriter;
import com.vsct.strowgr.monitoring.aggregator.cassandra.ParsedPayload;
import com.vsct.strowgr.monitoring.aggregator.cassandra.ParsedPayloadWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

public class MessageRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageRecorder.class);

    private final ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    private ParsedPayloadWriter writer;
    private ErrorRecordWriter   errorWriter;

    public MessageRecorder(ParsedPayloadWriter writer, ErrorRecordWriter errorWriter) {
        this.writer = writer;
        this.errorWriter = errorWriter;
    }

    public void record(Supplier<ParsedPayload> parsedPayloadSupplier, Function<Throwable, ErrorRecord> errorRecordSupplier) {
        service.submit(() -> {
            try {
                writer.write(parsedPayloadSupplier.get());
            } catch (Throwable t) {
                errorWriter.write(errorRecordSupplier.apply(t));
            }
        });
    }
}
