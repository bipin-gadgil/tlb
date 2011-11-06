package tlb.server.repo;

import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SuiteLevelEntry;
import tlb.domain.TimeProvider;
import tlb.utils.SystemEnvironment;

import java.io.*;
import java.util.*;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static tlb.server.repo.TestCaseRepo.TestCaseEntry.parseSingleEntry;

public class SuiteEntryRepoTest {

    private TestCaseRepo testCaseRepo;

    @Before
    public void setUp() {
        testCaseRepo = new TestCaseRepo(new TimeProvider());
    }

    @Test
    public void shouldStoreAttributesFactorySets() throws ClassNotFoundException, IOException {
        final EntryRepoFactory factory = new EntryRepoFactory(new SystemEnvironment(Collections.singletonMap(TlbConstants.Server.TLB_DATA_DIR.key, TestUtil.createTempFolder().getAbsolutePath())));
        final SuiteEntryRepo entryRepo = (SuiteEntryRepo) factory.findOrCreate("name_space", "version", "type", new EntryRepoFactory.Creator<SuiteEntryRepo>() {
            public SuiteEntryRepo create() {
                return new SuiteEntryRepo<TestCaseRepo.TestCaseEntry>() {
                    public Collection<TestCaseRepo.TestCaseEntry> list(String version) throws IOException, ClassNotFoundException {
                        return null;
                    }

                    public List<TestCaseRepo.TestCaseEntry> parse(String string) {
                        return TestCaseRepo.TestCaseEntry.parse(string);
                    }
                };
            }
        });
        assertThat(entryRepo.factory, sameInstance(factory));
        assertThat(entryRepo.namespace, is("name_space"));
        assertThat(entryRepo.identifier, is("name__space_version_type"));
    }

    @Test
    public void shouldNotAllowAdditionOfEntries() {
        try {
            testCaseRepo.add(parseSingleEntry("shouldBar#Bar"));
            fail("add should not have been allowed for suite repo");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage(), is("add not allowed on repository"));
        }
    }

    @Test
    public void shouldRecordSuiteRecordWhenUpdated() {
        testCaseRepo.update(parseSingleEntry("shouldBar#Bar"));
        testCaseRepo.update(parseSingleEntry("shouldFoo#Foo"));
        List<TestCaseRepo.TestCaseEntry> entryList = testCaseRepo.sortedList();
        assertThat(entryList.size(), is(2));
        assertThat((TestCaseRepo.TestCaseEntry) entryList.get(0), is(new TestCaseRepo.TestCaseEntry("shouldBar", "Bar")));
        assertThat((TestCaseRepo.TestCaseEntry) entryList.get(1), is(new TestCaseRepo.TestCaseEntry("shouldFoo", "Foo")));
    }

    @Test
    public void shouldOverwriteExistingEntryIfAddedAgain() {
        testCaseRepo.update(parseSingleEntry("shouldBar#Bar"));
        testCaseRepo.update(parseSingleEntry("shouldFoo#Foo"));
        testCaseRepo.update(parseSingleEntry("shouldBar#Foo"));
        List<TestCaseRepo.TestCaseEntry> entryList = testCaseRepo.sortedList();
        assertThat(entryList.size(), is(2));
        assertThat((TestCaseRepo.TestCaseEntry) entryList.get(0), is(new TestCaseRepo.TestCaseEntry("shouldBar", "Foo")));
        assertThat((TestCaseRepo.TestCaseEntry) entryList.get(1), is(new TestCaseRepo.TestCaseEntry("shouldFoo", "Foo")));
    }

    @Test
    public void shouldDumpDataOnGivenOutputStream() throws IOException, ClassNotFoundException {
        testCaseRepo.update(parseSingleEntry("shouldBar#Bar"));
        testCaseRepo.update(parseSingleEntry("shouldFoo#Foo"));
        String dump = testCaseRepo.diskDump();
        assertThat(dump, is("shouldBar#Bar\nshouldFoo#Foo\n"));
    }

    @Test
    public void shouldLoadFromDisk() throws IOException, ClassNotFoundException {
        testCaseRepo.loadCopyFromDisk("shouldBar#Bar\nshouldFoo#Foo\n");
        assertThat(testCaseRepo.sortedList(), is(listOf(new TestCaseRepo.TestCaseEntry("shouldBar", "Bar"), new TestCaseRepo.TestCaseEntry("shouldFoo", "Foo"))));
        assertThat(testCaseRepo.isDirty(), is(false));
    }

    @Test
    public void shouldLoadFromGivenReader() throws IOException, ClassNotFoundException {
        testCaseRepo.load("shouldBar#Bar\nshouldFoo#Foo\n");
        assertThat(testCaseRepo.sortedList(), is(listOf(new TestCaseRepo.TestCaseEntry("shouldBar", "Bar"), new TestCaseRepo.TestCaseEntry("shouldFoo", "Foo"))));
        assertThat(testCaseRepo.isDirty(), is(true));
    }

    @Test
    public void shouldVersionListItself() {
        testCaseRepo.update(parseSingleEntry("shouldBar#Bar"));
        testCaseRepo.update(parseSingleEntry("shouldFoo#Foo"));
        List<TestCaseRepo.TestCaseEntry> entryList = testCaseRepo.sortedList();
        assertThat(entryList.size(), is(2));
        assertThat(entryList.get(0), is(new TestCaseRepo.TestCaseEntry("shouldBar", "Bar")));
        assertThat(entryList.get(1), is(new TestCaseRepo.TestCaseEntry("shouldFoo", "Foo")));
    }

    @Test
    public void shouldUnderstandDirtiness() {
        assertThat(testCaseRepo.isDirty(), is(false));

        testCaseRepo.update(new TestCaseRepo.TestCaseEntry("should_run", "my.Suite"));
        assertThat(testCaseRepo.isDirty(), is(true));

        testCaseRepo.diskDump();
        assertThat(testCaseRepo.isDirty(), is(false));

        testCaseRepo.update(new TestCaseRepo.TestCaseEntry("should_bun", "my.Suite"));
        assertThat(testCaseRepo.isDirty(), is(true));

        testCaseRepo.loadCopyFromDisk("should_run#my.Suite\nshould_eat_bun#my.Suite");
        assertThat("Its not dirty if just loaded from file.", testCaseRepo.isDirty(), is(false));

        testCaseRepo.load("should_run#my.Suite\nshould_eat_bun#my.Suite");
        assertThat("Is dirty, as was loaded externally, and not from a file", testCaseRepo.isDirty(), is(true));
    }

    @Test
    public void shouldResetEntriesWhenLoadingFromString() {
        testCaseRepo.update(new TestCaseRepo.TestCaseEntry("test_name", "suite_name"));

        testCaseRepo.loadCopyFromDisk("foo#bar");

        Collection<TestCaseRepo.TestCaseEntry> list = testCaseRepo.list();
        assertThat(list.size(), is(1));
        assertThat(list.iterator().next(), is(new TestCaseRepo.TestCaseEntry("foo", "bar")));
    }

    private <T extends SuiteLevelEntry> List<T> listOf(T... entries) {
        ArrayList<T> list = new ArrayList<T>();
        for (T entry : entries) {
            list.add(entry);
        }
        return list;
    }
    
    @Test
    public void shouldUnderstandWhenHasFactorySet() {
        TestCaseRepo repo = new TestCaseRepo(new TimeProvider());
        assertThat(repo.hasFactory(), is(false));
        repo.setFactory(mock(EntryRepoFactory.class));
        assertThat(repo.hasFactory(), is(true));
        repo.setFactory(null);
        assertThat(repo.hasFactory(), is(false));
    }

    @Test
    public void shouldReturnIdentifier() {
        TestCaseRepo repo = new TestCaseRepo(new TimeProvider());
        assertThat(repo.getIdentifier(), is(nullValue()));
        repo.setIdentifier("foo");
        assertThat(repo.getIdentifier(), is("foo"));
    }
}
