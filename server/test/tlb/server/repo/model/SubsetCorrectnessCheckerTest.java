package tlb.server.repo.model;

import org.junit.Before;
import org.junit.Test;
import tlb.domain.SuiteNamePartitionEntry;
import tlb.domain.SuiteNamePartitionEntryTest;
import tlb.server.repo.PartitionRecordRepo;
import tlb.server.repo.SetRepo;
import tlb.server.repo.NamedEntryRepo;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SubsetCorrectnessCheckerTest {
    private SetRepo repo;
    private SubsetCorrectnessChecker checker;

    @Before
    public void setUp() throws Exception {
        repo = new SetRepo();
        checker = new SubsetCorrectnessChecker(repo, new PartitionRecordRepo());
    }

    @Test
    public void shouldRegisterWhenASubsetConsumesAGivenSuite() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");
        SetRepo.OperationResult result = checker.reportSubset("foo/bar/Baz\nfoo/Quux", 2, 10);
        assertThat(result.isSuccess(), is(true));

        List<SuiteNamePartitionEntry> sortedEntriesAfterSubsetPost = NamedEntryRepo.sortedListFor(repo.list());

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux");

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux", 2, 3, 10);

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 2, 3, 10);

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World");
    }

    @Test
    public void shouldFailWhen_subsetHasATestThatUniversalSetDoesNot() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");
        SetRepo.OperationResult result = checker.reportSubset("foo/bar/Baz\nfoo/Bar\nhell/Yeah\nhell/o/World\nfoo/Quux", 1, 2);
        assertThat(result.isSuccess(), is(false));
        assertThat(result.getMessage(), is("- Found 3 unknown(not present in universal set) suite(s) named: [foo/Bar, hell/Yeah, hell/o/World].\nHad total of 5 suites named [foo/Bar, foo/Quux, foo/bar/Baz, hell/Yeah, hell/o/World] in partition 1 of 2. Corresponding universal set had a total of 4 suites named [bar/baz/Quux, foo/Quux: 1/2, foo/bar/Baz: 1/2, hello/World].\n"));

        List<SuiteNamePartitionEntry> sortedEntriesAfterSubsetPost = NamedEntryRepo.sortedListFor(repo.list());

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux");

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux", 1, 2, 2);

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 1, 2, 2);

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World");
    }

    @Test
    public void shouldFailWhen_subsetHasATestAppearingMoreThanOnce() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");
        SetRepo.OperationResult result = checker.reportSubset("foo/Quux\nfoo/bar/Baz\nfoo/Quux\nfoo/bar/Baz\nfoo/Quux", 2, 3);
        assertThat(result.isSuccess(), is(false));
        assertThat(result.getMessage(), is("- Found more than one occurrence of 2 suite(s) named: {foo/bar/Baz=2, foo/Quux=3}.\nHad total of 5 suites named [foo/Quux, foo/Quux, foo/Quux, foo/bar/Baz, foo/bar/Baz] in partition 2 of 3. Corresponding universal set had a total of 4 suites named [bar/baz/Quux, foo/Quux: 2/3, foo/bar/Baz: 2/3, hello/World].\n"));

        List<SuiteNamePartitionEntry> sortedEntriesAfterSubsetPost = NamedEntryRepo.sortedListFor(repo.list());

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux");

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux", 2, 1, 3);

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 2, 1, 3);

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World");
    }
    
    @Test
    public void shouldFailWhen_mutualExclusionIsViolated() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");

        SetRepo.OperationResult result = checker.reportSubset("foo/bar/Baz\nbar/baz/Quux", 2, 3);
        assertThat(result.isSuccess(), is(true));

        result = checker.reportSubset("foo/Quux", 3, 3);
        assertThat(result.isSuccess(), is(true));

        result = checker.reportSubset("bar/baz/Quux\nhello/World\nfoo/Quux", 1, 3);
        assertThat(result.isSuccess(), is(false));
        assertThat(result.getMessage(), is("- Mutual exclusion of test-suites across splits violated by partition 1/3. Suites [bar/baz/Quux: 2/3, foo/Quux: 3/3] have already been selected for running by other partitions.\nHad total of 3 suites named [bar/baz/Quux, foo/Quux, hello/World] in partition 1 of 3. Corresponding universal set had a total of 4 suites named [bar/baz/Quux: 2/3, foo/Quux: 3/3, foo/bar/Baz: 2/3, hello/World: 1/3].\n"));
        List<SuiteNamePartitionEntry> sortedEntriesAfterSubsetPost = NamedEntryRepo.sortedListFor(repo.list());

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux", 2, 1, 3);

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux", 3, 2, 3);

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 2, 3, 3);

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World", 1, 2, 3);
    }
    
    @Test
    public void shouldNotFailWhen_oneSubsetPostsTwice() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");

        SetRepo.OperationResult result = checker.reportSubset("foo/bar/Baz\nbar/baz/Quux", 2, 3);
        assertThat(result.isSuccess(), is(true));

        List<SuiteNamePartitionEntry> sortedEntriesAfterSubsetPost = NamedEntryRepo.sortedListFor(repo.list());

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux", 2, 3, 3);

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux");

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 2, 3, 3);

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World");


        result = checker.reportSubset("foo/bar/Baz\nfoo/Quux", 2, 3); //second call from the same partition
        assertThat(result.isSuccess(), is(true));

        sortedEntriesAfterSubsetPost = NamedEntryRepo.sortedListFor(repo.list());

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux", 2, 3, 3);

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(1), "foo/Quux", 2, 3, 3);

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/bar/Baz", 2, 3, 3);

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "hello/World");
    }

    @Test
    public void shouldFailWhen_collectiveExhaustionIsViolated() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World\nbaz/bar/Foo");

        SetRepo.OperationResult result = checker.reportSubset("bar/baz/Quux", 2, 3);
        assertThat(result.isSuccess(), is(true));

        result = checker.reportSubset("foo/Quux", 3, 3);
        assertThat(result.isSuccess(), is(true));

        result = checker.reportSubset("hello/World", 1, 3);
        assertThat(result.isSuccess(), is(false));
        assertThat(result.getMessage(), is("- Collective exhaustion of tests violated with none of the 3 partition picked running suites: [baz/bar/Foo, foo/bar/Baz]. Failing partition 1 as this is the last one to execute.\nHad total of 1 suites named [hello/World] in partition 1 of 3. Corresponding universal set had a total of 5 suites named [bar/baz/Quux: 2/3, baz/bar/Foo, foo/Quux: 3/3, foo/bar/Baz, hello/World: 1/3].\n"));

        List<SuiteNamePartitionEntry> sortedEntriesAfterSubsetPost = NamedEntryRepo.sortedListFor(repo.list());

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(0), "bar/baz/Quux", 2, 1, 3);

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(1), "baz/bar/Foo");

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(2), "foo/Quux", 3, 2, 3);

        SuiteNamePartitionEntryTest.assertNotInUse(sortedEntriesAfterSubsetPost.get(3), "foo/bar/Baz");

        SuiteNamePartitionEntryTest.assertInUse(sortedEntriesAfterSubsetPost.get(4), "hello/World", 1, 2, 3);
    }
}
