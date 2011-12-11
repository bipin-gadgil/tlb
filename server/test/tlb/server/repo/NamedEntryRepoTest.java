package tlb.server.repo;

import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.TimeProvider;
import tlb.utils.SystemEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static tlb.server.repo.TestCaseRepo.TestCaseEntry.parseSingleEntry;

public class NamedEntryRepoTest {

    private TestCaseRepo testCaseRepo;

    @Before
    public void setUp() {
        testCaseRepo = new TestCaseRepo(new TimeProvider());
    }

    @Test
    public void shouldStoreAttributesFactorySets() throws ClassNotFoundException, IOException {
        final EntryRepoFactory factory = new EntryRepoFactory(new SystemEnvironment(Collections.singletonMap(TlbConstants.Server.TLB_DATA_DIR.key, TestUtil.createTempFolder().getAbsolutePath())));
        final NamedEntryRepo entryRepo = factory.findOrCreate("name_space", new EntryRepoFactory.VersionedNamespace("version", "type"), new EntryRepoFactory.Creator<NamedEntryRepo>() {
            public NamedEntryRepo create() {
                return new NamedEntryRepo<TestCaseRepo.TestCaseEntry>() {
                    public Collection<TestCaseRepo.TestCaseEntry> list(String version) throws IOException, ClassNotFoundException {
                        return null;
                    }

                    public List<TestCaseRepo.TestCaseEntry> parse(String string) {
                        return TestCaseRepo.TestCaseEntry.parse(string);
                    }
                };
            }
        }, null);
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

    @Test
    public void shouldWriteDataBackToFileWhenDirty_beforeGettingGarbageCollected() throws IllegalAccessException {
        EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        testCaseRepo.setFactory(repoFactory);
        testCaseRepo.setIdentifier("foo_bar_baz");

        testCaseRepo.update(new TestCaseRepo.TestCaseEntry("testFoo", "foo.Bar"));
        TestUtil.invoke("finalize", testCaseRepo);
        verify(repoFactory).syncRepoToDisk("foo_bar_baz", testCaseRepo);
    }

    @Test
    public void shouldNOTWriteDataBackToFileWhenNOTDirty_beforeGettingGarbageCollected() throws IllegalAccessException {
        EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        testCaseRepo.setFactory(repoFactory);
        testCaseRepo.setIdentifier("foo_bar_baz");

        testCaseRepo.update(new TestCaseRepo.TestCaseEntry("testFoo", "foo.Bar"));
        String ignore = testCaseRepo.diskDump();
        TestUtil.invoke("finalize", testCaseRepo);
        verify(repoFactory, never()).syncRepoToDisk("foo_bar_baz", testCaseRepo);
    }

    private <T> List<T> listOf(T... entries) {
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
