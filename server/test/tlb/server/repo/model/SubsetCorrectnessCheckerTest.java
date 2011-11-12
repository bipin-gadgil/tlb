package tlb.server.repo.model;

import org.junit.Before;
import org.junit.Test;
import tlb.domain.SuiteNameCountEntry;
import tlb.domain.SuiteNameCountEntryTest;
import tlb.domain.TimeProvider;
import tlb.server.repo.SetRepo;
import tlb.server.repo.SuiteEntryRepo;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class SubsetCorrectnessCheckerTest {
    private SetRepo repo;
    private SubsetCorrectnessChecker checker;

    @Before
    public void setUp() throws Exception {
        repo = new SetRepo(new TimeProvider());
        checker = new SubsetCorrectnessChecker(repo);
    }

    @Test
    public void shouldRegisterWhenASubsetConsumesAGivenSuite() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");
        SetRepo.OperationResult result = checker.reportSubset("foo/bar/Baz\nfoo/Quux", 2, 10);
        assertThat(result.success, is(true));
        assertThat(result.getMessage(), is(""));

        List<SuiteNameCountEntry> sortedEntriesAfterSubsetPost = SuiteEntryRepo.sortedListFor(repo.list());

        SuiteNameCountEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux");

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux", 2, 3, 10);

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 2, 3, 10);

        SuiteNameCountEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World");
    }

    @Test
    public void shouldFailWhen_subsetHasATestThatUniversalSetDoesNot() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");
        SetRepo.OperationResult result = checker.reportSubset("foo/bar/Baz\nfoo/Bar\nhell/Yeah\nhell/o/World\nfoo/Quux", 1, 2);
        assertThat(result.success, is(false));
        assertThat(result.getMessage(), is("- Found 3 unknown(not present in universal set) suite(s) named: [foo/Bar, hell/Yeah, hell/o/World].\nHad total of 5 suites named [foo/Bar, foo/Quux, foo/bar/Baz, hell/Yeah, hell/o/World] in partition 1 of 2. Corresponding universal set had a total of 4 suites named [bar/baz/Quux, foo/Quux: 1/2, foo/bar/Baz: 1/2, hello/World].\n"));

        List<SuiteNameCountEntry> sortedEntriesAfterSubsetPost = SuiteEntryRepo.sortedListFor(repo.list());

        SuiteNameCountEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux");

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux", 1, 2, 2);

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 1, 2, 2);

        SuiteNameCountEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World");
    }

    @Test
    public void shouldFailWhen_subsetHasATestAppearingMoreThanOnce() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");
        SetRepo.OperationResult result = checker.reportSubset("foo/Quux\nfoo/bar/Baz\nfoo/Quux\nfoo/bar/Baz\nfoo/Quux", 2, 3);
        assertThat(result.success, is(false));
        assertThat(result.getMessage(), is("- Found more than one occurrence of 2 suite(s) named: {foo/bar/Baz=2, foo/Quux=3}.\nHad total of 5 suites named [foo/Quux, foo/Quux, foo/Quux, foo/bar/Baz, foo/bar/Baz] in partition 2 of 3. Corresponding universal set had a total of 4 suites named [bar/baz/Quux, foo/Quux: 2/3, foo/bar/Baz: 2/3, hello/World].\n"));

        List<SuiteNameCountEntry> sortedEntriesAfterSubsetPost = SuiteEntryRepo.sortedListFor(repo.list());

        SuiteNameCountEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux");

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux", 2, 1, 3);

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 2, 1, 3);

        SuiteNameCountEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World");
    }
    
    @Test
    public void shouldFailWhen_twoDifferentSubsetsTryToRunTheSameTest() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");

        SetRepo.OperationResult result = checker.reportSubset("foo/bar/Baz\nbar/baz/Quux", 2, 3);
        assertThat(result.success, is(true));

        result = checker.reportSubset("foo/Quux", 3, 3);
        assertThat(result.success, is(true));

        result = checker.reportSubset("bar/baz/Quux\nhello/World\nfoo/Quux", 1, 3);
        assertThat(result.success, is(false));
        assertThat(result.getMessage(), is("- Mutual exclusion of test-suites across splits violated by partition 1/3. Suites [bar/baz/Quux: 2/3, foo/Quux: 3/3] have already been selected for running by other partitions.\nHad total of 3 suites named [bar/baz/Quux, foo/Quux, hello/World] in partition 1 of 3. Corresponding universal set had a total of 4 suites named [bar/baz/Quux: 2/3, foo/Quux: 3/3, foo/bar/Baz: 2/3, hello/World: 1/3].\n"));
        List<SuiteNameCountEntry> sortedEntriesAfterSubsetPost = SuiteEntryRepo.sortedListFor(repo.list());

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux", 2, 1, 3);

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux", 3, 2, 3);

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 2, 3, 3);

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World", 1, 2, 3);
    }
    
    @Test
    public void shouldNotFailWhen_oneSubsetPostsTwice() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");

        SetRepo.OperationResult result = checker.reportSubset("foo/bar/Baz\nbar/baz/Quux", 2, 3);
        assertThat(result.success, is(true));

        List<SuiteNameCountEntry> sortedEntriesAfterSubsetPost = SuiteEntryRepo.sortedListFor(repo.list());

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux", 2, 3, 3);

        SuiteNameCountEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux");

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 2, 3, 3);

        SuiteNameCountEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World");


        result = checker.reportSubset("foo/bar/Baz\nfoo/Quux", 2, 3); //second call from the same partition
        assertThat(result.success, is(true));

        sortedEntriesAfterSubsetPost = SuiteEntryRepo.sortedListFor(repo.list());

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux", 2, 3, 3);

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux", 2, 3, 3);

        SuiteNameCountEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 2, 3, 3);

        SuiteNameCountEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World");
    }
}
