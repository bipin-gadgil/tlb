package tlb.server.repo.model;

import org.junit.Before;
import org.junit.Test;
import tlb.domain.SuiteNameCountEntry;
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

        SuiteNameCountEntry bar_baz_Quux = sortedEntriesAfterSubsetPost.get(0);
        assertThat(bar_baz_Quux.getName(), is("bar/baz/Quux"));
        assertThat(bar_baz_Quux.getCount(), is((short) 1));

        SuiteNameCountEntry foo_Quux = sortedEntriesAfterSubsetPost.get(1);
        assertThat(foo_Quux.getName(), is("foo/Quux"));
        assertThat(foo_Quux.getCount(), is((short) 0));

        SuiteNameCountEntry foo_bar_Baz = sortedEntriesAfterSubsetPost.get(2);
        assertThat(foo_bar_Baz.getName(), is("foo/bar/Baz"));
        assertThat(foo_bar_Baz.getCount(), is((short) 0));

        SuiteNameCountEntry hello_World = sortedEntriesAfterSubsetPost.get(3);
        assertThat(hello_World.getName(), is("hello/World"));
        assertThat(hello_World.getCount(), is((short) 1));
    }

    @Test
    public void shouldFailWhen_subsetHasATestThatUniversalSetDoesNot() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");
        SetRepo.OperationResult result = checker.reportSubset("foo/bar/Baz\nfoo/Bar\nhell/Yeah\nhell/o/World\nfoo/Quux", 1, 2);
        assertThat(result.success, is(false));
        assertThat(result.getMessage(), is("- Found 3 unknown(not present in universal set) suite(s) named: [foo/Bar, hell/Yeah, hell/o/World].\nHad total of 5 suites named [foo/Bar, foo/Quux, foo/bar/Baz, hell/Yeah, hell/o/World] in partition 1 of 2. Corresponding universal set had a total of 4 suites named [bar/baz/Quux, foo/Quux, foo/bar/Baz, hello/World].\n"));

        List<SuiteNameCountEntry> sortedEntriesAfterSubsetPost = SuiteEntryRepo.sortedListFor(repo.list());

        SuiteNameCountEntry bar_baz_Quux = sortedEntriesAfterSubsetPost.get(0);
        assertThat(bar_baz_Quux.getName(), is("bar/baz/Quux"));
        assertThat(bar_baz_Quux.getCount(), is((short) 1));

        SuiteNameCountEntry foo_Quux = sortedEntriesAfterSubsetPost.get(1);
        assertThat(foo_Quux.getName(), is("foo/Quux"));
        assertThat(foo_Quux.getCount(), is((short) 0));

        SuiteNameCountEntry foo_bar_Baz = sortedEntriesAfterSubsetPost.get(2);
        assertThat(foo_bar_Baz.getName(), is("foo/bar/Baz"));
        assertThat(foo_bar_Baz.getCount(), is((short) 0));

        SuiteNameCountEntry hello_World = sortedEntriesAfterSubsetPost.get(3);
        assertThat(hello_World.getName(), is("hello/World"));
        assertThat(hello_World.getCount(), is((short) 1));
    }

    @Test
    public void shouldFailWhen_subsetHasATestAppearingMoreThanOnce() {
        repo.load("foo/bar/Baz\nbar/baz/Quux\nfoo/Quux\nhello/World");
        SetRepo.OperationResult result = checker.reportSubset("foo/Quux\nfoo/bar/Baz\nfoo/Quux\nfoo/bar/Baz\nfoo/Quux", 2, 3);
        assertThat(result.success, is(false));
        assertThat(result.getMessage(), is("- Found more than one occurrence of 2 suite(s) named: {foo/bar/Baz=2, foo/Quux=3}.\nHad total of 5 suites named [foo/Quux, foo/Quux, foo/Quux, foo/bar/Baz, foo/bar/Baz] in partition 2 of 3. Corresponding universal set had a total of 4 suites named [bar/baz/Quux, foo/Quux, foo/bar/Baz, hello/World].\n"));

        List<SuiteNameCountEntry> sortedEntriesAfterSubsetPost = SuiteEntryRepo.sortedListFor(repo.list());

        SuiteNameCountEntry bar_baz_Quux = sortedEntriesAfterSubsetPost.get(0);
        assertThat(bar_baz_Quux.getName(), is("bar/baz/Quux"));
        assertThat(bar_baz_Quux.getCount(), is((short) 1));

        SuiteNameCountEntry foo_Quux = sortedEntriesAfterSubsetPost.get(1);
        assertThat(foo_Quux.getName(), is("foo/Quux"));
        assertThat(foo_Quux.getCount(), is((short) 0));

        SuiteNameCountEntry foo_bar_Baz = sortedEntriesAfterSubsetPost.get(2);
        assertThat(foo_bar_Baz.getName(), is("foo/bar/Baz"));
        assertThat(foo_bar_Baz.getCount(), is((short) 0));

        SuiteNameCountEntry hello_World = sortedEntriesAfterSubsetPost.get(3);
        assertThat(hello_World.getName(), is("hello/World"));
        assertThat(hello_World.getCount(), is((short) 1));
    }
}
