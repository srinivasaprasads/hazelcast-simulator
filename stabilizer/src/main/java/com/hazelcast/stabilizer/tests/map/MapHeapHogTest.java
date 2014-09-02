package com.hazelcast.stabilizer.tests.map;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.core.Partition;
import com.hazelcast.core.PartitionService;
import com.hazelcast.instance.HazelcastInstanceProxy;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.stabilizer.tests.TestContext;
import com.hazelcast.stabilizer.tests.annotations.Performance;
import com.hazelcast.stabilizer.tests.annotations.Run;
import com.hazelcast.stabilizer.tests.annotations.Setup;
import com.hazelcast.stabilizer.tests.annotations.Verify;
import com.hazelcast.stabilizer.tests.annotations.Warmup;
import com.hazelcast.stabilizer.tests.utils.TestUtils;
import com.hazelcast.stabilizer.tests.utils.ThreadSpawner;

import java.util.concurrent.TimeUnit;

public class MapHeapHogTest {

    private final static ILogger log = Logger.getLogger(MapHeapHogTest.class);

    // properties
    public String basename = this.getClass().getName();
    public int threadCount = 3;
    public int ttlHours = 24;
    public double approxHeapUsageFactor = 0.9;

    private TestContext testContext;
    private HazelcastInstance targetInstance;

    // TODO: What is flag????
    private long flag = 0;
    private long approxEntryBytesSize = 238;

    private IMap map;

    @Setup
    public void setup(TestContext testContext) throws Exception {
        this.testContext = testContext;
        targetInstance = testContext.getTargetInstance();

        map = targetInstance.getMap(basename);
    }

    @Warmup(global = false)
    public void warmup() throws InterruptedException {
        if (isMemberNode(targetInstance)) {
            while (targetInstance.getCluster().getMembers().size() != 3) {
                log.info(basename + " waiting cluster == 3");
                Thread.sleep(1000);
            }

            TestUtils.warmupPartitions(log, targetInstance);
        }
    }

    @Run
    public void run() {
        ThreadSpawner spawner = new ThreadSpawner(testContext.getTestId());
        for (int k = 0; k < threadCount; k++) {
            spawner.spawn(new Worker());
        }
        spawner.awaitCompletion();
    }

    @Performance
    public long getOperationCount() {
        return flag;
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            while (!testContext.isStopped()) {

                long free = Runtime.getRuntime().freeMemory();
                long total = Runtime.getRuntime().totalMemory();
                long used = total - free;
                long max = Runtime.getRuntime().maxMemory();
                long totalFree = max - used;

                long maxLocalEntries = (long) ((totalFree / approxEntryBytesSize) * approxHeapUsageFactor);

                long key = 0;
                for (int i = 0; i < maxLocalEntries; i++) {
                    key = nextKeyOwnedBy(key, targetInstance);
                    map.put(key, key, ttlHours, TimeUnit.HOURS);
                    key++;
                }
                log.info(basename + " after warmUp map size = " + map.size());
                log.info(basename + " putCount = " + maxLocalEntries);
                printMemStats();

                log.info("added All");
                flag = 1;
            }
        }
    }

    @Verify(global = false)
    public void localVerify() throws Exception {
        if (isMemberNode(targetInstance)) {
            log.info(basename + " verify map size = " + map.size());
            printMemStats();
        }
    }

    public void printMemStats() {
        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        long used = total - free;
        long max = Runtime.getRuntime().maxMemory();
        double usedOfMax = 100.0 * ((double) used / (double) max);

        long totalFree = max - used;

        log.info(basename + " free = " + TestUtils.humanReadableByteCount(free, true) + " = " + free);
        log.info(basename + " total free = " + TestUtils.humanReadableByteCount(totalFree, true) + " = " + totalFree);
        log.info(basename + " used = " + TestUtils.humanReadableByteCount(used, true) + " = " + used);
        log.info(basename + " max = " + TestUtils.humanReadableByteCount(max, true) + " = " + max);
        log.info(basename + " usedOfMax = " + usedOfMax + "%");
    }

    public static long nextKeyOwnedBy(long key, HazelcastInstance instance) {
        final Member localMember = instance.getCluster().getLocalMember();
        final PartitionService partitionService = instance.getPartitionService();
        for (; ; ) {
            Partition partition = partitionService.getPartition(key);
            if (localMember.equals(partition.getOwner())) {
                return key;
            }
            key++;
        }
    }

    public static boolean isMemberNode(HazelcastInstance instance) {
        return instance instanceof HazelcastInstanceProxy;
    }

}
