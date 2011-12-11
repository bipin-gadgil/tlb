package tlb.server.repo;

import tlb.domain.SuiteTimeEntry;
import tlb.domain.TimeProvider;

import java.util.List;

/**
 * @understands storage and retrival of time that each suite took to run
 */
public class SuiteTimeRepo extends NamedEntryRepo<SuiteTimeEntry> {
    public List<SuiteTimeEntry> parse(String string) {
        return SuiteTimeEntry.parse(string);
    }
}
