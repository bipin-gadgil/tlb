package tlb.domain;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RepoCreatedTimeEntryTest {
    @Test
    public void shouldParseList() {
        List<RepoCreatedTimeEntry> parsed = RepoCreatedTimeEntry.parse("foo: 100\nbar: 20\nbaz:10\n");
        assertThat(parsed.size(), is(3));
        assertThat(parsed.get(0), is(new RepoCreatedTimeEntry("foo", 100l)));
        assertThat(parsed.get(1), is(new RepoCreatedTimeEntry("bar", 20l)));
        assertThat(parsed.get(2), is(new RepoCreatedTimeEntry("baz", 10l)));
    }
}
