package io.tradecraft.observability.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.log.LogUtils;
import io.tradecraft.common.meta.Component;
import io.tradecraft.common.meta.Flow;
import io.tradecraft.common.meta.MessageType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultTraceWriter implements TraceWriter {

    private final ArrayBlockingQueue<Envelope<?>> queue;
    private final ObjectMapper mapper;
    private final BufferedWriter writer;
    private final Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public DefaultTraceWriter(String name, Path path, int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);

        this.mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .findAndRegisterModules();

        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            this.writer = Files.newBufferedWriter(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new RuntimeException("Unable to open trace path: " + path, e);
        }

        this.worker = new Thread(this::runWriter, "tradecraft-traceWriter-" + name);
        this.worker.setDaemon(false);
        this.worker.start();
    }

    @Override
    public void write(Envelope<?> env) {
        try {
            queue.put(env); // deterministic, blocks only when full
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while adding trace event", e);
        }
    }

    private void runWriter() {
        while (running.get() || !queue.isEmpty()) {
            try {
                Envelope<?> env = queue.poll(10, TimeUnit.MILLISECONDS);
                if (env != null) {
                    String json = mapper.writeValueAsString(env);
                    writer.write(json);
                    writer.newLine();
                    writer.flush();
                }
            } catch (Exception e) {
                LogUtils.logEx(Component.COMMON, MessageType.ADMIN, Flow.NA, this, e, "Trace writer error");
            }
        }

        try { writer.flush(); } catch (Exception e) {
            LogUtils.logEx(Component.COMMON, MessageType.ADMIN, Flow.NA, this, e, "Trace writer error");
        }
        try { writer.close(); } catch (Exception e) {
            LogUtils.logEx(Component.COMMON, MessageType.ADMIN, Flow.NA, this, e, "Trace writer error");
        }
    }

    @Override
    public void close() throws Exception {
        running.set(false);
        worker.join();
        LogUtils.log(Component.COMMON, MessageType.ADMIN, Flow.NA, this, "Trace writer close");
    }
}

