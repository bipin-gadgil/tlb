package tlb.server.repo;

import org.junit.Test;
import tlb.domain.RepoCreatedTimeEntry;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RepoLedgerTest {
    @Test
    public void shouldParseMultipleLedgerEntries() {
        List<RepoCreatedTimeEntry> parsedList = new RepoLedger().parse("foo_bar_baz: 10\nbar_baz_quux: 20");
        assertThat(parsedList.get(0), is(new RepoCreatedTimeEntry("foo_bar_baz", 10l)));
        assertThat(parsedList.get(1), is(new RepoCreatedTimeEntry("bar_baz_quux", 20l)));
        assertThat(parsedList.size(), is(2));
    }
}
