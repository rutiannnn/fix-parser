package fix.parser.impl;

import fix.parser.message.base.FixMessage;
import fix.parser.messages44.NewOrderSingleMessage;
import fix.parser.spec.FixSpec;
import fix.parser.spec.FixSpecParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;

public class FixParserBenchmark {
    private static final int WARMUP_ITERATIONS = 100_000;
    private static final int BENCHMARK_ITERATIONS = 1000_000;
    private static final Runtime RUNTIME = Runtime.getRuntime();

    private final FixMessageParser parser;
    private final byte[] simpleMessage;
    private final byte[] complexMessage;
    private final NewOrderSingleMessage parsedSimpleMessage;
    private final NewOrderSingleMessage parsedComplexMessage;

    public FixParserBenchmark() throws Exception {
        // Initialize parser
        File specFile = new File("../../fix-parser-messages44/src/main/resources/FIX44.xml");
        FixSpecParser specParser = new FixSpecParser(specFile);
        FixSpec spec = specParser.parse();
        parser = new FixMessageParser(spec, "fix.parser.messages44");

        // Simple message with basic fields
        simpleMessage = (
            "8=FIX.4.4\u00019=176\u000135=D\u000149=BUYER\u000156=SELLER\u000134=1\u0001" +
                "52=20230615-14:30:00.000\u000111=123456\u000155=IBM\u000154=1\u000144=150.25\u0001" +
                "38=1000\u000110=128\u0001"
        ).getBytes(StandardCharsets.ISO_8859_1);

        // Complex message with repeating groups
        complexMessage = (
            "8=FIX.4.4\u00019=200\u000135=D\u000149=BUYER\u000156=SELLER\u000134=1\u0001" +
                "52=20230615-14:30:00.000\u000111=123456\u000155=IBM\u000154=1\u000144=150.25\u0001" +
                "38=1000\u0001453=2\u0001448=TRADER1\u0001447=D\u0001452=1\u0001" +
                "448=TRADER2\u0001447=D\u0001452=2\u000110=128\u0001"
        ).getBytes(StandardCharsets.ISO_8859_1);

        // Pre-parse messages for getter benchmarks
        parsedSimpleMessage = (NewOrderSingleMessage) parser.parse(simpleMessage);
        parsedComplexMessage = (NewOrderSingleMessage) parser.parse(complexMessage);
    }

    private void runBenchmark(String name, Runnable benchmark) {
        System.out.println("\nRunning benchmark: " + name);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            benchmark.run();
        }

        // Force GC before measurement
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Measure memory before
        long memoryBefore = RUNTIME.totalMemory() - RUNTIME.freeMemory();

        // Collect timing samples
        List<Long> samples = new ArrayList<>(BENCHMARK_ITERATIONS);
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            benchmark.run();
            samples.add(System.nanoTime() - start);
        }

        // Measure memory after
        long memoryAfter = RUNTIME.totalMemory() - RUNTIME.freeMemory();

        // Calculate statistics
        LongSummaryStatistics stats = samples.stream().mapToLong(Long::valueOf).summaryStatistics();

        // Print results
        System.out.printf("Average time: %.2f ns%n", stats.getAverage());
        System.out.printf("Min time: %d ns%n", stats.getMin());
        System.out.printf("Max time: %d ns%n", stats.getMax());
        System.out.printf("Memory delta: %.2f MB%n", (memoryAfter - memoryBefore) / (1024.0 * 1024.0));
    }

    public void runAllBenchmarks() {
        // Parsing benchmarks
        runBenchmark("Parse Simple Message", () -> {
            FixMessage message = parser.parse(simpleMessage);
            preventOptimization(message);
        });

        runBenchmark("Parse Complex Message", () -> {
            FixMessage message = parser.parse(complexMessage);
            preventOptimization(message);
        });

        // Field access benchmarks
        runBenchmark("Get String Field (Symbol)", () -> {
            String symbol = parsedSimpleMessage.getInstrument().getSymbol();
            preventOptimization(symbol);
        });

        runBenchmark("Get Double Field (Price)", () -> {
            double price = parsedSimpleMessage.getPrice();
            preventOptimization(price);
        });

        runBenchmark("Get Int Field (OrderQty)", () -> {
            double orderQty = parsedSimpleMessage.getOrderQtyData().getOrderQty();
            preventOptimization(orderQty);
        });

        runBenchmark("Get Repeating Group (Parties)", () -> {
            var parties = parsedComplexMessage.getParties().getPartyIDs();
            preventOptimization(parties);
        });

        runBenchmark("Get All Fields", () -> {
            String symbol = parsedSimpleMessage.getInstrument().getSymbol();
            double price = parsedSimpleMessage.getPrice();
            double orderQty = parsedSimpleMessage.getOrderQtyData().getOrderQty();
            String clOrdId = parsedSimpleMessage.getClOrdID();
            char side = parsedSimpleMessage.getSide();
            preventOptimization(symbol, price, orderQty, clOrdId, side);
        });
    }

    // Prevent JVM from optimizing away unused values
    private static volatile Object preventOptimizationSink;

    private static void preventOptimization(Object... objects) {
        preventOptimizationSink = objects[objects.length - 1];
    }

    public static void main(String[] args) throws Exception {
        FixParserBenchmark benchmark = new FixParserBenchmark();
        benchmark.runAllBenchmarks();
    }
}