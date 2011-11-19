package tlb.domain;

import java.util.List;

/**
 * @understands association a repo identifier to time it was created
 */
public class RepoCreatedTimeEntry extends NameNumberEntry {
    public static final EntryCreator<RepoCreatedTimeEntry> REPO_INSTANCE_CREATOR = new EntryCreator<RepoCreatedTimeEntry>() {
        public RepoCreatedTimeEntry create(String name, long number) {
            return new RepoCreatedTimeEntry(name, number);
        }
    };

    public RepoCreatedTimeEntry(String name, long number) {
        super(name, number);
    }

    public static List<RepoCreatedTimeEntry> parse(String entries) {
        return parse(entries, REPO_INSTANCE_CREATOR);
    }
}
