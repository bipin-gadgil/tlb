package tlb.service;

import org.hamcrest.core.Is;
import org.junit.*;
import org.junit.matchers.JUnitMatchers;
import org.restlet.Component;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.server.ServerInitializer;
import tlb.server.TlbServerInitializer;
import tlb.service.http.DefaultHttpAction;
import tlb.splitter.correctness.ValidationResult;
import tlb.utils.SystemEnvironment;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static tlb.TestUtil.updateEnv;

/**
 * This in addition to being a talk-to-tlb-server test is also a tlb-server integration test,
 * hence uses real instance of tlb server
 */
public class TlbServerTest {
    private static Component component;
    private TlbServer server;
    private static String freePort;
    private HashMap<String,String> clientEnv;
    private DefaultHttpAction httpAction;
    private SystemEnvironment env;
    private String jobVersion;
    private String jobName;
    private String partitionNumber;
    private String totalPartitions;

    @BeforeClass
    public static void startTlbServer() throws Exception {
        HashMap<String, String> serverEnv = new HashMap<String, String>();
        serverEnv.put(TlbConstants.TLB_SMOOTHING_FACTOR.key, "0.1");
        freePort = TestUtil.findFreePort();
        serverEnv.put(TlbConstants.Server.TLB_SERVER_PORT.key, freePort);
        serverEnv.put(TlbConstants.Server.TLB_DATA_DIR.key, TestUtil.createTempFolder().getAbsolutePath());
        ServerInitializer main = new TlbServerInitializer(new SystemEnvironment(serverEnv));
        component = main.init();
        component.start();
    }

    @AfterClass
    public static void shutDownTlbServer() throws Exception {
        component.stop();
    }

    @Before
    public void setUp() {
        clientEnv = new HashMap<String, String>();
        jobName = "job";
        clientEnv.put(TlbConstants.TlbServer.TLB_JOB_NAME, jobName);
        partitionNumber = "4";
        clientEnv.put(TlbConstants.TlbServer.TLB_PARTITION_NUMBER, partitionNumber);
        totalPartitions = "15";
        clientEnv.put(TlbConstants.TlbServer.TLB_TOTAL_PARTITIONS, totalPartitions);
        jobVersion = String.valueOf(UUID.randomUUID());
        clientEnv.put(TlbConstants.TlbServer.TLB_JOB_VERSION, jobVersion);
        String url = "http://localhost:" + freePort;
        clientEnv.put(TlbConstants.TlbServer.TLB_BASE_URL, url);
        httpAction = new DefaultHttpAction();
        env = new SystemEnvironment(clientEnv);
        server = new TlbServer(env, httpAction);
        server.clearCachingFiles();
    }

    @Test
    public void shouldBeAbleToPostSubsetSize() throws NoSuchFieldException, IllegalAccessException {
        server.publishSubsetSize(10);
        server.publishSubsetSize(20);
        server.publishSubsetSize(17);
        Assert.assertThat(httpAction.get(String.format("http://localhost:%s/job-4/subset_size", freePort)), Is.is("10\n20\n17\n"));
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "5");
        server.publishSubsetSize(12);
        server.publishSubsetSize(13);
        Assert.assertThat(httpAction.get(String.format("http://localhost:%s/job-5/subset_size", freePort)), Is.is("12\n13\n"));
    }

    @Test
    public void shouldBeAbleToPostSuiteTime() throws NoSuchFieldException, IllegalAccessException {
        server.testClassTime("com.foo.Foo", 100);
        server.testClassTime("com.bar.Bar", 120);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "2");
        server.testClassTime("com.baz.Baz", 15);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "15");
        server.testClassTime("com.quux.Quux", 137);
        final String response = httpAction.get(String.format("http://localhost:%s/job/suite_time", freePort));
        final List<SuiteTimeEntry> entryList = SuiteTimeEntry.parse(response);
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 100)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 120)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 15)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 137)));
    }

    @Test
    public void shouldBeAbleToPostSmoothenedSuiteTimeToRepo() throws NoSuchFieldException, IllegalAccessException {
        updateEnv(env, TlbConstants.TlbServer.TLB_JOB_NAME, "foo-job");
        updateEnv(env, TlbConstants.TLB_SMOOTHING_FACTOR.key, "0.5");
        String suiteTimeUrl = String.format("http://localhost:%s/foo-job/suite_time", freePort);
        httpAction.put(suiteTimeUrl, "com.foo.Foo: 100");
        httpAction.put(suiteTimeUrl, "com.bar.Bar: 120");
        httpAction.put(suiteTimeUrl, "com.quux.Quux: 20");

        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "4");
        server.testClassTime("com.foo.Foo", 200);
        server.testClassTime("com.bar.Bar", 160);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "2");
        server.testClassTime("com.baz.Baz", 15);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "15");
        server.testClassTime("com.quux.Quux", 160);
        String response = httpAction.get(suiteTimeUrl);
        List<SuiteTimeEntry> entryList = SuiteTimeEntry.parse(response);
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 150)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 140)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 15)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 90)));
    }

    @Test
    public void shouldBeAbleToPostSuiteResult() throws NoSuchFieldException, IllegalAccessException {
        server.testClassFailure("com.foo.Foo", true);
        server.testClassFailure("com.bar.Bar", false);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "2");
        server.testClassFailure("com.baz.Baz", true);
        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "15");
        server.testClassFailure("com.quux.Quux", true);
        final String response = httpAction.get(String.format("http://localhost:%s/job/suite_result", freePort));
        final List<SuiteResultEntry> entryList = SuiteResultEntry.parse(response);
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.foo.Foo", true)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.bar.Bar", false)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.baz.Baz", true)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.quux.Quux", true)));
    }

    @Test
    public void shouldBeAbleToFetchSuiteTimesUnaffectedByFurtherUpdates() throws NoSuchFieldException, IllegalAccessException {
        final String url = String.format("http://localhost:%s/job/suite_time", freePort);
        httpAction.put(url, "com.foo.Foo: 10");
        httpAction.put(url, "com.bar.Bar: 12");
        httpAction.put(url, "com.baz.Baz: 17");
        httpAction.put(url, "com.quux.Quux: 150");

        List<SuiteTimeEntry> entryList = server.getLastRunTestTimes();
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 10)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));

        server = new TlbServer(env, httpAction);
        httpAction.put(url, "com.foo.Foo: 18");
        httpAction.put(url, "com.foo.Bang: 103");

        entryList = server.getLastRunTestTimes();
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 10)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));

        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "2");
        server = new TlbServer(env, httpAction);
        entryList = server.getLastRunTestTimes();
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 10)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));

        //should fetch latest for unknown version
        updateEnv(env, TlbConstants.TlbServer.TLB_JOB_VERSION, "bar");
        server = new TlbServer(env, httpAction);
        entryList = server.getLastRunTestTimes();
        Assert.assertThat(entryList.size(), Is.is(5));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Foo", 18)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.bar.Bar", 12)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.baz.Baz", 17)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.quux.Quux", 150)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteTimeEntry("com.foo.Bang", 103)));
    }

    @Test
    public void shouldBeAbleToFetchSuiteResults() throws NoSuchFieldException, IllegalAccessException {
        final String url = String.format("http://localhost:%s/job/suite_result", freePort);
        httpAction.put(url, "com.foo.Foo: true");
        httpAction.put(url, "com.bar.Bar: false");
        httpAction.put(url, "com.baz.Baz: false");
        httpAction.put(url, "com.quux.Quux: true");

        List<SuiteResultEntry> entryList = server.getLastRunFailedTests();
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.foo.Foo", true)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.bar.Bar", false)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.baz.Baz", false)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.quux.Quux", true)));

        updateEnv(env, TlbConstants.TlbServer.TLB_PARTITION_NUMBER, "2");
        entryList = server.getLastRunFailedTests();
        Assert.assertThat(entryList.size(), Is.is(4));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.foo.Foo", true)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.bar.Bar", false)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.baz.Baz", false)));
        Assert.assertThat(entryList, JUnitMatchers.hasItem(new SuiteResultEntry("com.quux.Quux", true)));
    }
    
    @Test
    public void shouldReadTotalPartitionsFromEnvironmentVariables() throws NoSuchFieldException, IllegalAccessException {
        Assert.assertThat(server.totalPartitions(), Is.is(15));
        updateEnv(env, TlbConstants.TlbServer.TLB_TOTAL_PARTITIONS, "7");
        Assert.assertThat(server.totalPartitions(), Is.is(7));
    }

    @Test
    public void shouldPostUniversalSetToServer_underModuleNamespace_ForFirstPartition() throws IllegalAccessException {
        ArrayList<TlbSuiteFile> files = new ArrayList<TlbSuiteFile>();
        files.add(new TlbSuiteFileImpl("com.foo.Foo"));
        files.add(new TlbSuiteFileImpl("com.bar.Bar"));
        files.add(new TlbSuiteFileImpl("com.baz.Baz"));
        files.add(new TlbSuiteFileImpl("com.quux.Quux"));

        ValidationResult validationResult = server.validateUniversalSet(files, "foo-module");

        assertThat(validationResult.hasFailed(), is(false));
        assertThat(validationResult.getMessage(), is("First validation snapshot."));
        assertThat((ValidationResult.Status) TestUtil.deref("status", validationResult), is(ValidationResult.Status.FIRST));

        validationResult = server.validateUniversalSet(files, "bar-module");

        assertThat(validationResult.hasFailed(), is(false));
        assertThat(validationResult.getMessage(), is("First validation snapshot."));
        assertThat((ValidationResult.Status) TestUtil.deref("status", validationResult), is(ValidationResult.Status.FIRST));
    }

    @Test
    public void shouldReturnFailure_whenUniversalSetMismatches() throws IllegalAccessException {
        ArrayList<TlbSuiteFile> files = new ArrayList<TlbSuiteFile>();
        files.add(new TlbSuiteFileImpl("com.foo.Foo"));
        files.add(new TlbSuiteFileImpl("com.bar.Bar"));
        files.add(new TlbSuiteFileImpl("com.baz.Baz"));
        files.add(new TlbSuiteFileImpl("com.quux.Quux"));

        ValidationResult validationResult = server.validateUniversalSet(files, "foo-module");

        assertThat(validationResult.hasFailed(), is(false));
        assertThat(validationResult.getMessage(), is("First validation snapshot."));
        assertThat((ValidationResult.Status) TestUtil.deref("status", validationResult), is(ValidationResult.Status.FIRST));

        files.remove(0);
        incrementPartitionNumber();
        validationResult = server.validateUniversalSet(files, "foo-module");

        assertThat(validationResult.hasFailed(), is(true));
        assertThat(validationResult.getMessage(), is("Expected universal set was [com.bar.Bar, com.baz.Baz, com.foo.Foo, com.quux.Quux] but given [com.bar.Bar, com.baz.Baz, com.quux.Quux]."));
        assertThat((ValidationResult.Status) TestUtil.deref("status", validationResult), is(ValidationResult.Status.FAILED));
    }

    @Test
    public void shouldReturnFailure_whenUniversalSetMatchesVersionPostedByOtherPartitions() throws IllegalAccessException {
        ArrayList<TlbSuiteFile> files = new ArrayList<TlbSuiteFile>();
        files.add(new TlbSuiteFileImpl("com.foo.Foo"));
        files.add(new TlbSuiteFileImpl("com.bar.Bar"));
        files.add(new TlbSuiteFileImpl("com.baz.Baz"));
        files.add(new TlbSuiteFileImpl("com.quux.Quux"));

        ValidationResult validationResult = server.validateUniversalSet(files, "foo-module");

        assertThat(validationResult.hasFailed(), is(false));
        assertThat(validationResult.getMessage(), is("First validation snapshot."));
        assertThat((ValidationResult.Status) TestUtil.deref("status", validationResult), is(ValidationResult.Status.FIRST));

        incrementPartitionNumber();

        validationResult = server.validateUniversalSet(files, "foo-module");

        assertThat(validationResult.hasFailed(), is(false));
        assertThat(validationResult.getMessage(), is("Universal set matched."));
        assertThat((ValidationResult.Status) TestUtil.deref("status", validationResult), is(ValidationResult.Status.OK));
    }

    private void incrementPartitionNumber() {
        clientEnv.put(TlbConstants.TlbServer.TLB_PARTITION_NUMBER, String.valueOf(Integer.parseInt(partitionNumber) + 1));
    }


    private String correctnessCheckUrl(String checkType, String moduleName) {
        return String.format("http://localhost:%s/%s/%s/%s/%s/%s", jobName, TlbConstants.Server.EntryRepoFactory.CORRECTNESS_CHECK, jobVersion, totalPartitions, partitionNumber, checkType, moduleName);
    }
}
