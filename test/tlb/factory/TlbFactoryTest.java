package tlb.factory;

import org.hamcrest.core.Is;
import org.junit.Test;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.balancer.BalancerInitializer;
import tlb.orderer.FailedFirstOrderer;
import tlb.orderer.TestOrderer;
import tlb.server.ServerInitializer;
import tlb.server.TlbServerInitializer;
import tlb.service.TalkToCruise;
import tlb.service.TalkToService;
import tlb.service.TalkToTlbServer;
import tlb.splitter.*;
import tlb.utils.SystemEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TlbFactoryTest {

    @Test
    public void shouldReturnDefaultMatchAllCriteriaForEmpty() {
        TestSplitterCriteria criteria = TlbFactory.getCriteria(null, env("tlb.service.TalkToCruise"));
        assertThat(criteria, Is.is(JobFamilyAwareSplitterCriteria.MATCH_ALL_FILE_SET));
        criteria = TlbFactory.getCriteria("", env("tlb.service.TalkToCruise"));
        assertThat(criteria, is(JobFamilyAwareSplitterCriteria.MATCH_ALL_FILE_SET));
    }
    
    @Test
    public void shouldReturnNoOPOrdererForEmpty() {
        TestOrderer orderer = TlbFactory.getOrderer(null, env("tlb.service.TalkToCruise"));
        assertThat(orderer, Is.is(TestOrderer.NO_OP));
        orderer = TlbFactory.getOrderer("", env("tlb.service.TalkToCruise"));
        assertThat(orderer, is(TestOrderer.NO_OP));
    }

    @Test
    public void shouldThrowAnExceptionWhenTheCriteriaClassIsNotFound() {
        try {
            TlbFactory.getCriteria("com.thoughtworks.cruise.tlb.MissingCriteria", env("tlb.service.TalkToCruise"));
            fail("should not be able to create random criteria!");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Unable to locate class 'com.thoughtworks.cruise.tlb.MissingCriteria'"));
        }

        try {
            TlbFactory.getOrderer("com.thoughtworks.cruise.tlb.MissingOrderer", env("tlb.service.TalkToCruise"));
            fail("should not be able to create random orderer!");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Unable to locate class 'com.thoughtworks.cruise.tlb.MissingOrderer'"));
        }
    }

    @Test
    public void shouldThrowExceptionIfClassByGivenNameNotAssignableToInterfaceFactoryIsCreatedWith() {
        final TlbFactory<ServerInitializer> factory = new TlbFactory<ServerInitializer>(ServerInitializer.class, null);
        try {
            factory.getInstance("tlb.balancer.TlbClient", new SystemEnvironment());
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Class 'tlb.balancer.TlbClient' is-not/does-not-implement 'class tlb.server.ServerInitializer'"));
        }
    }
    
    @Test
    public void shouldThrowAnExceptionWhenTheCriteriaClassDoesNotImplementTestSplitterCriteria() {
        try {
            TlbFactory.getCriteria("java.lang.String", env("tlb.service.TalkToCruise"));
            fail("should not be able to create criteria that doesn't implement TestSplitterCriteria");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Class 'java.lang.String' is-not/does-not-implement 'class tlb.splitter.TestSplitterCriteria'"));
        }
    }

    @Test
    public void shouldReturnCountBasedCriteria() {
        TestSplitterCriteria criteria = TlbFactory.getCriteria("tlb.splitter.CountBasedTestSplitterCriteria", env("tlb.service.TalkToCruise"));
        assertThat(criteria, instanceOf(CountBasedTestSplitterCriteria.class));
    }
    
    @Test
    public void shouldInjectTlbCommunicatorWhenImplementsTalkToService() {
        TlbFactory<TestSplitterCriteria> criteriaFactory = new TlbFactory<TestSplitterCriteria>(TestSplitterCriteria.class, JobFamilyAwareSplitterCriteria.MATCH_ALL_FILE_SET);
        TestSplitterCriteria criteria = criteriaFactory.getInstance(MockCriteria.class, env("tlb.service.TalkToTlbServer"));
        assertThat(criteria, instanceOf(MockCriteria.class));
        assertThat(((MockCriteria)criteria).calledTalksToService, is(true));
        assertThat(((MockCriteria)criteria).talker, is(TalkToTlbServer.class));
    }

    @Test
    public void shouldInjectCruiseCommunicatorWhenImplementsTalkToService() {
        TlbFactory<TestSplitterCriteria> criteriaFactory = new TlbFactory<TestSplitterCriteria>(TestSplitterCriteria.class, JobFamilyAwareSplitterCriteria.MATCH_ALL_FILE_SET);
        TestSplitterCriteria criteria = criteriaFactory.getInstance(MockCriteria.class, env("tlb.service.TalkToCruise"));
        assertThat(criteria, instanceOf(MockCriteria.class));
        assertThat(((MockCriteria)criteria).calledTalksToService, is(true));
        assertThat(((MockCriteria)criteria).talker, is(TalkToCruise.class));
    }

    @Test
    public void shouldReturnTimeBasedCriteria() {
        TestSplitterCriteria criteria = TlbFactory.getCriteria("tlb.splitter.TimeBasedTestSplitterCriteria", env("tlb.service.TalkToCruise"));
        assertThat(criteria, instanceOf(TimeBasedTestSplitterCriteria.class));
    }

    @Test
    public void shouldReturnFailedFirstOrderer() {
        TestOrderer failedTestsFirstOrderer = TlbFactory.getOrderer("tlb.orderer.FailedFirstOrderer", env("tlb.service.TalkToCruise"));
        assertThat(failedTestsFirstOrderer, instanceOf(FailedFirstOrderer.class));
    }

    @Test
    public void shouldReturnTalkToTlbServer() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(TlbConstants.TlbServer.URL, "http://localhost:7019");
        map.put(TlbConstants.TALK_TO_SERVICE, "tlb.service.TalkToTlbServer");
        TalkToService talkToService = TlbFactory.getTalkToService(new SystemEnvironment(map));
        assertThat(talkToService, is(TalkToTlbServer.class));
    }
    
    @Test
    public void shouldReturnTalkToCruise() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(TlbConstants.Cruise.CRUISE_SERVER_URL, "http://localhost:8153/cruise");
        map.put(TlbConstants.TALK_TO_SERVICE, "tlb.service.TalkToCruise");
        TalkToService talkToService = TlbFactory.getTalkToService(new SystemEnvironment(map));
        assertThat(talkToService, is(TalkToCruise.class));
    }

    @Test
    public void shouldReturnTlbServerRestletLauncher() {
        ServerInitializer launcher = TlbFactory.getRestletLauncher("tlb.server.TlbServerInitializer", new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(TlbServerInitializer.class));
    }

    @Test
    public void shouldReturnBalancerRestletLauncher() {
        ServerInitializer launcher = TlbFactory.getRestletLauncher("tlb.balancer.BalancerInitializer", new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(BalancerInitializer.class));
    }
    
    @Test
    public void shouldReturnTlbServerRestletLauncherIfNoneGiven() {
        ServerInitializer launcher = TlbFactory.getRestletLauncher(null, new SystemEnvironment(new HashMap<String, String>()));
        assertThat(launcher, is(TlbServerInitializer.class));
    }

    private SystemEnvironment env(String talkToService) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TlbConstants.Cruise.CRUISE_SERVER_URL, "https://localhost:8154/cruise");
        map.put(TlbConstants.TlbServer.URL, "http://localhost:7019");
        map.put(TlbConstants.TALK_TO_SERVICE, talkToService);
        return new SystemEnvironment(map);
    }

    private static class MockCriteria extends JobFamilyAwareSplitterCriteria implements TalksToService {
        private boolean calledFilter = false;
        private boolean calledTalksToService = false;
        private TalkToService talker;

        public MockCriteria(SystemEnvironment env) {
            super(env);
        }

        protected List<TlbSuiteFile> subset(List<TlbSuiteFile> fileResources) {
            this.calledFilter = true;
            return null;
        }

        public void talksToService(TalkToService service) {
            this.calledTalksToService = true;
            this.talker = service;
        }
    }
}
