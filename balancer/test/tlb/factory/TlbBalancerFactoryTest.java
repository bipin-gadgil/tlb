package tlb.factory;

import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.orderer.FailedFirstOrderer;
import tlb.orderer.TestOrderer;
import tlb.service.GoServer;
import tlb.service.Server;
import tlb.service.TalksToServer;
import tlb.service.TlbServer;
import tlb.splitter.*;
import tlb.splitter.correctness.AbortOnFailure;
import tlb.splitter.correctness.NoOp;
import tlb.splitter.correctness.SplitChecker;
import tlb.utils.SystemEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TlbBalancerFactoryTest {

    @Test
    public void shouldReturnDefaultMatchAllCriteriaForEmpty() throws IllegalAccessException {
        TestSplitter criteria = TlbBalancerFactory.getCriteria(null, env("tlb.service.GoServer", null));
        assertThat(criteria, is(NoOp.class));
        assertThat((TestSplitter) TestUtil.deref("splitter", criteria), is(JobFamilyAwareSplitter.MATCH_ALL_FILE_SET));
        criteria = TlbBalancerFactory.getCriteria("", env("tlb.service.GoServer", null));
        assertThat((TestSplitter) TestUtil.deref("splitter", criteria), is(JobFamilyAwareSplitter.MATCH_ALL_FILE_SET));
    }

    @Test
    public void shouldReturnNoOPOrdererForEmpty() {
        TestOrderer orderer = TlbBalancerFactory.getOrderer(null, env("tlb.service.GoServer", null));
        assertThat(orderer, is(TestOrderer.NO_OP));
        orderer = TlbBalancerFactory.getOrderer("", env("tlb.service.GoServer", null));
        assertThat(orderer, is(TestOrderer.NO_OP));
    }

    @Test
    public void shouldThrowAnExceptionWhenTheCriteriaClassIsNotFound() {
        try {
            TlbBalancerFactory.getCriteria("com.thoughtworks.cruise.tlb.MissingCriteria", env("tlb.service.GoServer", null));
            fail("should not be able to create random criteria!");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Unable to locate class 'com.thoughtworks.cruise.tlb.MissingCriteria'"));
        }

        try {
            TlbBalancerFactory.getOrderer("com.thoughtworks.cruise.tlb.MissingOrderer", env("tlb.service.GoServer", null));
            fail("should not be able to create random orderer!");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Unable to locate class 'com.thoughtworks.cruise.tlb.MissingOrderer'"));
        }
    }


    @Test
    public void shouldThrowAnExceptionWhenTheCriteriaClassDoesNotImplementTestSplitterCriteria() {
        try {
            TlbBalancerFactory.getCriteria("java.lang.String", env("tlb.service.GoServer", null));
            fail("should not be able to create criteria that doesn't implement TestSplitter");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Class 'java.lang.String' is-not/does-not-implement 'interface tlb.splitter.TestSplitter'"));
        }
    }

    @Test
    public void shouldReturn_CorrectnessCheckDecoratedCriteria() throws IllegalAccessException {
        TestSplitter criteria = TlbBalancerFactory.getCriteria("tlb.splitter.CountBasedTestSplitter", env("tlb.service.TlbServer", "tlb.splitter.correctness.AbortOnFailure"));
        assertThat(criteria, instanceOf(AbortOnFailure.class));
        assertThat(TestUtil.deref("splitter", criteria), instanceOf(CountBasedTestSplitter.class));
    }

    @Test
    public void shouldReturnCountBasedCriteria() throws IllegalAccessException {
        TestSplitter criteria = TlbBalancerFactory.getCriteria("tlb.splitter.CountBasedTestSplitter", env("tlb.service.GoServer", null));
        assertThat(criteria, instanceOf(SplitChecker.class));
        assertThat(TestUtil.deref("splitter", criteria), instanceOf(CountBasedTestSplitter.class));
    }

    @Test
    public void shouldInjectTlbCommunicatorWhenImplementsTalkToService() {
        TlbFactory<TestSplitter> criteriaFactory = new TlbFactory<TestSplitter>(TestSplitter.class, JobFamilyAwareSplitter.MATCH_ALL_FILE_SET, SystemEnvironment.class);
        SystemEnvironment env = env("tlb.service.TlbServer", null);
        TestSplitter criteria = criteriaFactory.getInstance(MockSplitter.class, env, env);
        assertThat(criteria, instanceOf(MockSplitter.class));
        assertThat(((MockSplitter)criteria).calledTalksToService, is(true));
        assertThat(((MockSplitter)criteria).talker, is(TlbServer.class));
    }

    @Test
    public void shouldInjectCruiseCommunicatorWhenImplementsTalkToService() {
        TlbFactory<TestSplitter> criteriaFactory = new TlbFactory<TestSplitter>(TestSplitter.class, JobFamilyAwareSplitter.MATCH_ALL_FILE_SET, SystemEnvironment.class);
        SystemEnvironment env = env("tlb.service.GoServer", null);
        TestSplitter criteria = criteriaFactory.getInstance(MockSplitter.class, env, env);
        assertThat(criteria, instanceOf(MockSplitter.class));
        assertThat(((MockSplitter)criteria).calledTalksToService, is(true));
        assertThat(((MockSplitter)criteria).talker, is(GoServer.class));
    }

    @Test
    public void shouldReturnTimeBasedCriteria() throws IllegalAccessException {
        TestSplitter criteria = TlbBalancerFactory.getCriteria("tlb.splitter.TimeBasedTestSplitter", env("tlb.service.GoServer", null));
        assertThat(criteria, instanceOf(SplitChecker.class));
        assertThat(TestUtil.deref("splitter", criteria), instanceOf(TimeBasedTestSplitter.class));
    }

    @Test
    public void shouldReturnFailedFirstOrderer() {
        TestOrderer failedTestsFirstOrderer = TlbBalancerFactory.getOrderer("tlb.orderer.FailedFirstOrderer", env("tlb.service.GoServer", null));
        assertThat(failedTestsFirstOrderer, instanceOf(FailedFirstOrderer.class));
    }

    @Test
    public void shouldReturnTalkToTlbServer() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(TlbConstants.TlbServer.TLB_BASE_URL, "http://localhost:7019");
        map.put(TlbConstants.TYPE_OF_SERVER.key, "tlb.service.TlbServer");
        Server server = TlbFactory.getTalkToService(new SystemEnvironment(map));
        assertThat(server, is(TlbServer.class));
    }
    
    @Test
    public void shouldReturnTalkToCruise() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(TlbConstants.Go.GO_SERVER_URL, "http://localhost:8153/cruise");
        map.put(TlbConstants.TYPE_OF_SERVER.key, "tlb.service.GoServer");
        Server server = TlbFactory.getTalkToService(new SystemEnvironment(map));
        assertThat(server, is(GoServer.class));
    }

    private SystemEnvironment env(String talkToService, final String checker) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TlbConstants.Go.GO_SERVER_URL, "https://localhost:8154/cruise");
        map.put(TlbConstants.TlbServer.TLB_BASE_URL, "http://localhost:7019");
        map.put(TlbConstants.TYPE_OF_SERVER.key, talkToService);
        map.put(TlbConstants.Correctness.SPLIT_CORRECTNESS_CHECKER.key, checker);
        return new SystemEnvironment(map);
    }

    private static class MockSplitter extends JobFamilyAwareSplitter implements TalksToServer {
        private boolean calledFilter = false;
        private boolean calledTalksToService = false;
        private Server talker;

        public MockSplitter(SystemEnvironment env) {
            super(env);
        }

        protected List<TlbSuiteFile> subset(List<TlbSuiteFile> fileResources) {
            this.calledFilter = true;
            return null;
        }

        public void talksToServer(Server service) {
            this.calledTalksToService = true;
            this.talker = service;
        }
    }
}
