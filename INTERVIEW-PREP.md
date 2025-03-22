# Garbage Collection

## Types of GC Algorithms

1. Serial GC: Single-threaded, suitable for small heaps. STW.
    - Best for: Small heaps (<4GB), single-CPU systems
    - Key params: -XX:+UseSerialGC, -XX:NewSize, -XX:MaxNewSize
    - Trade-off: Simple but stops-the-world for all collections
    - How it works:
        1. Uses mark-sweep-compact for old gen
        2. Uses copying collector for young gen
        3. Single thread performs all GC work
        4. Young gen collection: Copies live objects from Eden to Survivor
        5. Old gen collection: Mark-sweep-compact in three phases
        6. Full STW during both young and old collections

2. Parallel GC: Multi-threaded, suitable for medium-sized heaps. STW.
    - Best for: Batch processing, high throughput requirements
    - Key params: -XX:+UseParallelGC, -XX:ParallelGCThreads=N, -XX:MaxGCPauseMillis
    - Trade-off: Better throughput but still has STW pauses
    - How it works:
        1. Multiple threads for both young and old gen collection
        2. Young gen: Parallel copying collector
        3. Old gen: Parallel mark-sweep-compact
        4. Adaptive sizing adjusts heap sizes automatically
        5. Uses generational collection like Serial GC
        6. Full STW but faster due to parallel threads

3. Concurrent Mark Sweep (CMS) GC: Low pause times, suitable for large heaps.
    - Best for: Response time sensitive apps with large heaps
    - Key params: -XX:+UseConcMarkSweepGC, -XX:CMSInitiatingOccupancyFraction
    - Trade-off: Lower pauses but uses more CPU and memory
    - How it works:
        1. Initial Mark (STW): Mark GC roots
        2. Concurrent Mark: Trace object graph concurrently
        3. Remark (STW): Handle changes during concurrent mark
        4. Concurrent Sweep: Remove garbage concurrently
        5. No compaction (leads to fragmentation)
        6. Uses ParNew (parallel) for young generation
        7. Concurrent mode failures trigger full STW collection

4. Garbage First (G1) GC: Balances pause times and throughput, suitable for large heaps.
    - Best for: Large heaps (>4GB), balanced latency/throughput
    - Key params: -XX:+UseG1GC, -XX:MaxGCPauseMillis, -XX:G1HeapRegionSize
    - Trade-off: Good overall performance but higher memory overhead
    - How it works:
        1. Divides heap into equal-sized regions (1-32MB)
        2. Regions can be Eden, Survivor, Old, or Humongous
        3. Maintains remembered sets for cross-region references
        4. Collection phases:
            - Young GC: Collects Eden and Survivor regions
            - Mixed GC: Collects young + selected old regions
            - Full GC: Traditional full collection if needed
        5. Uses concurrent marking to identify garbage regions
        6. Prioritizes regions with most garbage (garbage first)

5. Z Garbage Collector (ZGC): Low pause times, suitable for large heaps.
    - Best for: Ultra-low latency, very large heaps
    - Key params: -XX:+UseZGC, -XX:ZCollectionInterval, -XX:ConcGCThreads
    - Trade-off: Lowest pauses but higher CPU usage
    - How it works:
        1. Uses colored pointers (4 bits in 64-bit references)
        2. Load barriers for concurrent operations
        3. No separate generations (single generation)
        4. Phases:
            - Pause Mark Start (STW): Very short pause
            - Concurrent Mark/Remap: Main work
            - Pause Mark End (STW): Very short pause
            - Concurrent Reference Processing
            - Concurrent Reset Relocation Set
        5. Concurrent compaction and relocation
        6. Sub-millisecond pauses regardless of heap size

6. Shenandoah GC: Low pause times, suitable for large heaps.
    - Best for: Ultra-low latency, similar to ZGC
    - Key params: -XX:+UseShenandoahGC, -XX:ShenandoahGCHeuristics
    - Trade-off: Very low pauses but higher CPU usage
    - How it works:
        1. Uses Brooks pointers (forwarding pointer in object header)
        2. Read barriers for concurrent operations
        3. No generations (like ZGC)
        4. Phases:
            - Initial Mark (STW): Mark roots
            - Concurrent Mark: Trace object graph
            - Final Mark (STW): Handle changes
            - Concurrent Evacuation: Copy live objects
            - Initial Update Refs (STW): Update root references
            - Concurrent Update References: Update all references
            - Final Update Refs (STW): Finish updates
        5. Concurrent compaction during evacuation
        6. Adaptive heuristics for collection timing

7. Epsilon GC: No GC, suitable for testing and benchmarking.
    - Best for: Performance testing, short-lived apps
    - Key params: -XX:+UseEpsilonGC
    - Trade-off: No GC overhead but will crash when memory full
    - How it works:
        1. Allocates memory from heap
        2. No garbage collection at all
        3. No memory reclamation
        4. Crashes with OutOfMemoryError when heap full
        5. Useful for:
            - Memory allocation profiling
            - GC overhead comparison
            - Short-lived testing
            - Performance benchmarking

## Important GC Terms and Concepts

1. Stop-the-World (STW): Period when application threads are paused for GC
2. Young Generation: Where new objects are allocated
3. Old Generation: Where long-lived objects are stored
4. Eden Space: Part of young generation where objects are initially created
5. Survivor Spaces: Buffer between Eden and Old generation
6. Marking: Process of identifying live objects
7. Sweeping: Process of reclaiming dead object space
8. Compaction: Process of reducing heap fragmentation
9. Concurrent: GC work happening alongside application threads
10. Parallel: Multiple GC threads working simultaneously

## Common GC Parameters

1. Memory Settings:
    - -Xms: Initial heap size
    - -Xmx: Maximum heap size
    - -XX:MetaspaceSize: Initial metaspace size
    - -XX:MaxMetaspaceSize: Maximum metaspace size

2. Sizing Parameters:
    - -XX:NewSize: Initial young generation size
    - -XX:MaxNewSize: Maximum young generation size
    - -XX:SurvivorRatio: Eden/Survivor space ratio
    - -XX:NewRatio: Young/Old generation ratio

3. Behavioral Parameters:
    - -XX:+AlwaysPreTouch: Touch all heap pages at startup
    - -XX:+DisableExplicitGC: Disable System.gc() calls
    - -XX:MaxGCPauseMillis: Target max GC pause time
    - -XX:GCTimeRatio: Ratio of GC/application time

4. Logging and Monitoring:
    - -Xlog:gc: Basic GC logging
    - -Xlog:gc*: Detailed GC logging
    - -Xlog:gc+heap=debug: Heap details
    - -Xlog:gc+phases=debug: GC phases

## Object Header

1. Mark Word: synchronization state, identity hash code, age bits for GC. Bits in mark word used for:
    - Lock state (2 bits)
    - Biased locking (1 bit)
    - Age for GC (4 bits)
    - Identity hashcode (25 bits)
    - Thread ID for biased locking (23 bits)
    - Epoch for biased locking (2 bits)
    - Unused (1 bit)
2. Klass Pointer: points to the class object of the instance (4 bytes on 32bit-JVMs, 8 bytes on 64bit-JVMs)

## Key Parameter: AlwaysPreTouch

-XX:+AlwaysPreTouch forces the JVM to touch all heap pages during initialization:

1. Purpose: Maps virtual memory to physical memory at startup
2. Benefits:
    - More predictable latency (no runtime page faults)
    - Better NUMA behavior
    - Consistent performance
3. Trade-offs:
    - Longer startup time
    - Higher initial memory usage
4. Best for:
    - Low-latency applications
    - Production systems
    - Performance benchmarking
5. Not recommended for:
    - Development environments
    - Memory-constrained systems
