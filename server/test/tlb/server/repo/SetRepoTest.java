package tlb.server.repo;

import org.junit.Before;
import org.junit.Test;
import tlb.domain.SuiteNameCountEntry;
import tlb.domain.TimeProvider;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SetRepoTest {

    private SetRepo repo;

    @Before
    public void setUp() throws Exception {
        repo = new SetRepo(new TimeProvider());
    }

    @Test
    public void shouldUnderstandParsingMultipleEntries() {
        List<SuiteNameCountEntry> parsedList = repo.parse("foo/bar/Baz\nbaz/bang/Quux\nhello/World");
        assertThat(parsedList, is(Arrays.asList(new SuiteNameCountEntry("foo/bar/Baz"), new SuiteNameCountEntry("baz/bang/Quux"), new SuiteNameCountEntry("hello/World"))));
        for (SuiteNameCountEntry suiteNameCountEntry : parsedList) {
            assertThat(suiteNameCountEntry.getCount(), is((short) 1));
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
        repo.update(new SuiteNameCountEntry("foo"));
    }
}
