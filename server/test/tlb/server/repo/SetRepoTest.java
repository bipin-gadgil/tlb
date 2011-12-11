package tlb.server.repo;

import org.junit.Before;
import org.junit.Test;
import tlb.domain.SuiteNamePartitionEntry;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class SetRepoTest {

    private SetRepo repo;

    @Before
    public void setUp() throws Exception {
        repo = new SetRepo();
    }

    @Test
    public void shouldUnderstandParsingMultipleEntries() {
        List<SuiteNamePartitionEntry> parsedList = repo.parse("foo/bar/Baz\nbaz/bang/Quux\nhello/World");
        assertThat(parsedList, is(Arrays.asList(new SuiteNamePartitionEntry("foo/bar/Baz"), new SuiteNamePartitionEntry("baz/bang/Quux"), new SuiteNamePartitionEntry("hello/World"))));
        for (SuiteNamePartitionEntry suiteNamePartitionEntry : parsedList) {
            assertThat(suiteNamePartitionEntry.isUsedByAnyPartition(), is(false));
        }
    }

    @Test
    public void shouldUnderstandIfRepoHasBeenPrimedWithData() {
        assertThat(repo.isPrimed(), is(false));
        repo.load("foo/bar/Baz\nbar/baz/Quux");
        assertThat(repo.isPrimed(), is(true));
        String s = repo.diskDump();
        assertThat(repo.isPrimed(), is(true));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotAllowUpdate() {
        repo.update(new SuiteNamePartitionEntry("foo"));
    }
}
