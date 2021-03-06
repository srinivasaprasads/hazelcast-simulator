package com.hazelcast.simulator.coordinator;

import com.hazelcast.simulator.agent.workerjvm.WorkerJvmSettings;
import com.hazelcast.simulator.coordinator.remoting.AgentsClient;
import com.hazelcast.simulator.test.TestCase;
import com.hazelcast.simulator.test.TestPhase;
import com.hazelcast.simulator.test.TestSuite;
import com.hazelcast.simulator.worker.commands.Command;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoordinatorRunTestSuiteTest {

    private static String userDir;

    private final TestSuite testSuite = new TestSuite();

    @Mock
    private final WorkerJvmSettings workerJvmSettings = new WorkerJvmSettings();

    @Mock
    private final AgentsClient agentsClient = mock(AgentsClient.class);

    @Mock
    private final FailureMonitor failureMonitor = mock(FailureMonitor.class);

    @Mock
    private final PerformanceMonitor performanceMonitor = mock(PerformanceMonitor.class);

    @InjectMocks
    private Coordinator coordinator;

    @BeforeClass
    public static void setUp() throws Exception {
        userDir = System.getProperty("user.dir");
        System.setProperty("user.dir", "./dist/src/main/dist");
    }

    @AfterClass
    public static void tearDown() {
        System.setProperty("user.dir", userDir);
    }

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        List<String> privateAddressList = new ArrayList<String>(1);
        privateAddressList.add("127.0.0.1");

        TestCase testCase1 = new TestCase("CoordinatorTest1");
        TestCase testCase2 = new TestCase("CoordinatorTest2");

        testSuite.addTest(testCase1);
        testSuite.addTest(testCase2);

        coordinator.testSuite = testSuite;

        when(agentsClient.getPublicAddresses()).thenReturn(privateAddressList);
        when(agentsClient.getAgentCount()).thenReturn(1);

        when(failureMonitor.getFailureCount()).thenReturn(0);
    }

    @Test
    public void runTestSuiteParallel() throws Exception {
        coordinator.parallel = true;

        coordinator.runTestSuite();

        verifyAgentsClient(2);
    }

    @Test
    public void runTestSuiteSequential() throws Exception {
        coordinator.parallel = false;

        coordinator.runTestSuite();

        verifyAgentsClient(2);
    }

    private void verifyAgentsClient(int numberOfTests) throws Exception {
        int phaseNumber = 8;

        verify(agentsClient, times(phaseNumber * numberOfTests)).executeOnAllWorkers(any(Command.class));
        verify(agentsClient, times(phaseNumber))
                .waitForPhaseCompletion(anyString(), eq("CoordinatorTest1"), any(TestPhase.class));
        verify(agentsClient, times(phaseNumber))
                .waitForPhaseCompletion(anyString(), eq("CoordinatorTest2"), any(TestPhase.class));
        verify(agentsClient, times(1)).terminateWorkers();
    }
}
