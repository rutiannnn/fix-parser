package fix.parser.benchmark;

import fix.parser.FixParser;
import fix.parser.api.FixMessage;
import jdk.jfr.*;

import java.nio.file.Path;

public class FixParserBenchmark {
    private static final String SAMPLE_MESSAGE =
            "8=FIX.4.2\u00019=145\u000135=D\u000134=4\u000149=SENDER\u0001" +
                    "52=20170901-16:21:00\u000156=TARGET\u000111=order-1\u000121=1\u0001" +
                    "55=SYMBOL\u000154=1\u000140=1\u000159=0\u000144=10.5\u000138=100\u0001";

    @Name("fix.parser.ParseEvent")
    @Label("FIX Parse Event")
    @Category("FIX Parser")
    static class ParseEvent extends Event {
        @Label("Message Size")
        int messageSize;

        @Label("Parse Time (ns)")
        long parseTimeNanos;
    }

    @Name("fix.parser.FieldAccessEvent")
    @Label("FIX Field Access Event")
    @Category("FIX Parser")
    static class FieldAccessEvent extends Event {
        @Label("Field Tag")
        int tag;

        @Label("Access Time (ns)")
        long accessTimeNanos;
    }

    public static void main(String[] args) throws Exception {
        // Create and start JFR recording
        Recording recording = startRecording();

        try {
            runBenchmark();
        } finally {
            // Stop and dump recording
            recording.stop();
            // Create out directory if it doesn't exist
            Path outDir = Path.of("out");
            if (!outDir.toFile().exists()) {
                outDir.toFile().mkdirs();
            }

            // Dump recording to out directory
            recording.dump(outDir.resolve("fix-parser-benchmark.jfr"));
        }
    }

    private static Recording startRecording() throws Exception {
        Configuration config = Configuration.getConfiguration("profile");

        Recording recording = new Recording(config);
        recording.enable("fix.parser.*")
                .withStackTrace();

        recording.start();
        return recording;
    }

    private static void runBenchmark() {
        byte[] message = SAMPLE_MESSAGE.getBytes();
        int iterations = 1_000_000;

        // Warm up JVM
        System.out.println("Warming up...");
        warmup(message);

        // Benchmark parsing only
        System.out.println("\nBenchmarking message parsing...");
        benchmarkParsing(message, iterations);

        // Benchmark parsing + field access
        System.out.println("\nBenchmarking parsing + field access...");
        benchmarkParsingAndAccess(message, iterations);
    }

    private static void warmup(byte[] message) {
        long methodStartTime = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            FixMessage msg = FixParser.parse(message);
            msg.getString(55);
            msg.getDouble(44);
            msg.getInt(38);
        }
        long totalTime = System.nanoTime() - methodStartTime;
        printStats("Warmup", 100_000, totalTime);
    }

    private static void benchmarkParsing(byte[] message, int iterations) {
        ParseEvent event = new ParseEvent();
        long methodStartTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            event.messageSize = message.length;
            long startTime = System.nanoTime();
            event.begin();

            FixMessage msg = FixParser.parse(message);

            event.end();
            event.parseTimeNanos = System.nanoTime() - startTime;
            event.commit();
        }

        long totalTime = System.nanoTime() - methodStartTime;
        printStats("Parsing only", iterations, totalTime);
    }

    private static void benchmarkParsingAndAccess(byte[] message, int iterations) {
        ParseEvent parseEvent = new ParseEvent();
        FieldAccessEvent accessEvent = new FieldAccessEvent();
        long methodStartTime = System.nanoTime();
        int i = 0;

        while (i < iterations) {
            parseEvent.messageSize = message.length;
            long startTime = System.nanoTime();
            parseEvent.begin();

            FixMessage msg = FixParser.parse(message);

            parseEvent.end();
            parseEvent.parseTimeNanos = System.nanoTime() - startTime;
            parseEvent.commit();

            // Measure field access
            measureFieldAccess(msg, 55, accessEvent); // Symbol
            measureFieldAccess(msg, 44, accessEvent); // Price
            measureFieldAccess(msg, 38, accessEvent); // Quantity

            i++;
        }

        long totalTime = System.nanoTime() - methodStartTime;
        printStats("Parsing + field access", iterations, totalTime);
    }

    private static void measureFieldAccess(FixMessage msg, int tag, FieldAccessEvent event) {
        event.tag = tag;
        long startTime = System.nanoTime();
        event.begin();

        switch (tag) {
            case 55 -> msg.getString(tag);
            case 44 -> msg.getDouble(tag);
            case 38 -> msg.getInt(tag);
        }

        event.end();
        event.accessTimeNanos = System.nanoTime() - startTime;
        event.commit();
    }

    private static void printStats(String operation, int iterations, long totalNanos) {
        double avgNanos = totalNanos / (double) iterations;
        double opsPerSec = (1_000_000_000.0 / avgNanos);

        System.out.printf("""
                        Results for %s:
                        Total time: %.2f ms
                        Avg time per message: %.2f ns
                        Operations per second: %.2f million
                        """,
                operation,
                totalNanos / 1_000_000.0,
                avgNanos,
                opsPerSec / 1_000_000.0
        );
    }
}