package tlb.server.repo;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.internal.verification.Times;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.TimeProvider;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static tlb.server.repo.EntryRepoFactory.LATEST_VERSION;
import static tlb.server.repo.TestCaseRepo.TestCaseEntry.parseSingleEntry;

public class VersioningEntryRepoTest {
    private TestCaseRepo repo;
    protected EntryRepoFactory factory;
    protected File tmpDir;

    private SystemEnvironment env() {
        final HashMap<String, String> env = new HashMap<String, String>();
        env.put(TlbConstants.Server.TLB_DATA_DIR.key, tmpDir.getAbsolutePath());
        return new SystemEnvironment(env);
    }


    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        tmpDir = TestUtil.createTempFolder();
        factory = new EntryRepoFactory(env());
        repo = createRepo(factory);
        repo.update(parseSingleEntry("shouldBar#Bar"));
        repo.update(parseSingleEntry("shouldBaz#Baz"));
    }

    private void stubTime(TimeProvider timeProvider, GregorianCalendar cal) {
        when(timeProvider.cal()).thenReturn(cal);
        when(timeProvider.now()).thenReturn(cal.getTime());
    }

    @Test
    public void shouldReturnListAsAtTheTimeOfQueryingAVersion() throws ClassNotFoundException, IOException {
        final List<TestCaseRepo.TestCaseEntry> frozenCollection = repo.sortedList("foo");
        assertThat(frozenCollection.size(), is(2));
        assertThat(frozenCollection, hasItem(new TestCaseRepo.TestCaseEntry("shouldBar", "Bar")));
        assertThat(frozenCollection, hasItem(new TestCaseRepo.TestCaseEntry("shouldBaz", "Baz")));
    }

    @Test
    public void shouldFreezeAVersionOnceCreated() throws ClassNotFoundException, IOException {
        repo.list("foo");
        repo.update(parseSingleEntry("shouldFoo#Foo"));
        repo.update(parseSingleEntry("shouldBaz#Quux"));
        final List<TestCaseRepo.TestCaseEntry> frozenCollection = repo.sortedList("foo");
        assertThat(frozenCollection.size(), is(2));
        assertThat(frozenCollection, hasItem(new TestCaseRepo.TestCaseEntry("shouldBar", "Bar")));
        assertThat(frozenCollection, hasItem(new TestCaseRepo.TestCaseEntry("shouldBaz", "Baz")));
    }

    @Test
    public void shouldKeepVersionFrozenAcrossDumpAndReload() throws InterruptedException, ClassNotFoundException, IOException {
        repo.list("foo");
        repo.update(parseSingleEntry("shouldFoo#Foo"));
        repo.update(parseSingleEntry("shouldBaz#Quux"));
        final Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();
        final TestCaseRepo newTestCaseRepo = createRepo(new EntryRepoFactory(env()));
        final List<TestCaseRepo.TestCaseEntry> frozenCollection = newTestCaseRepo.sortedList("foo");
        assertThat(frozenCollection.size(), is(2));
        assertThat(frozenCollection, hasItem(new TestCaseRepo.TestCaseEntry("shouldBar", "Bar")));
        assertThat(frozenCollection, hasItem(new TestCaseRepo.TestCaseEntry("shouldBaz", "Baz")));
    }

    @Test
    public void shouldSetFactoryAndNamespace() {
        assertThat(repo.getNamespace(), is("foo"));
        assertThat(repo.getFactory(), sameInstance(factory));
    }
    
    private TestCaseRepo createRepo(final EntryRepoFactory factory) throws IOException, ClassNotFoundException {
        return factory.findOrCreate("foo", new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, "test_case"), new EntryRepoFactory.Creator<TestCaseRepo>() {
            public TestCaseRepo create() {
                return new TestCaseRepo(new TimeProvider());
            }
        }, null);
    }
}
