package tlb.server.repo;

import tlb.domain.RepoCreatedTimeEntry;

import java.util.List;

/**
 * @understands maintaining record of created repos(identifiers) and creation time
 */
public class RepoLedger extends NamedEntryRepo<RepoCreatedTimeEntry> {
    public List<RepoCreatedTimeEntry> parse(String string) {
        return RepoCreatedTimeEntry.parse(string);
    }

    public void deleteRepoEntryFor(String identifier) {
        nameToEntry.remove(identifier);
    }
}
