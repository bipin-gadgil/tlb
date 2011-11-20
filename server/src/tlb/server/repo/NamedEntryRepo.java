package tlb.server.repo;

import tlb.domain.NamedEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @understands persistence and retrieval of suite based data
 */
public abstract class NamedEntryRepo<T extends NamedEntry> implements EntryRepo<T> {
    protected volatile Map<String, T> nameToEntry;
    protected String namespace;
    transient protected EntryRepoFactory factory;
    protected volatile String identifier;
    private volatile boolean dirty;

    public NamedEntryRepo() {
        super();
        nameToEntry = new ConcurrentHashMap<String, T>();
    }

    public List<T> sortedList() {
        return sortedListFor(list());
    }

    public static <T extends NamedEntry> List<T> sortedListFor(Collection<T> list) {
        List<T> entryList = new ArrayList<T>(list);
        Collections.sort(entryList, new Comparator<NamedEntry>() {
            public int compare(NamedEntry o1, NamedEntry o2) {
                return getKey(o1).compareTo(getKey(o2));
            }
        });
        return entryList;
    }

    public Collection<T> list() {
        return nameToEntry.values();
    }

    public void update(T record) {
        nameToEntry.put(getKey(record), record);
        dirty = true;
    }

    protected static String getKey(NamedEntry record) {
        return record.getName();
    }

    public final void add(T entry) {
        throw new UnsupportedOperationException("add not allowed on repository");
    }

    public final void setFactory(EntryRepoFactory factory) {
        this.factory = factory;
    }

    public final boolean hasFactory() {
        return factory != null;
    }

    //TODO: kill these two setters(namespace and identifier), pass data in one shot to set both internally
    public final void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setIdentifier(String type) {
        this.identifier = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isDirty() {
        return dirty;
    }

    public final String diskDump() {
        dirty = false;
        return dump();
    }

    public final String dump() {
        StringBuilder dumpBuffer = new StringBuilder();
        for (T entry : nameToEntry.values()) {
            dumpBuffer.append(entry.dump());
        }
        return dumpBuffer.toString();
    }

    public void loadCopyFromDisk(final String fileContents) {
        dirty = false;
        loadInternal(fileContents);
    }

    public void load(String contents) {
        loadInternal(contents);
        dirty = true;
    }

    private void loadInternal(String contents) {
        nameToEntry.clear();
        for (T entry : parse(contents)) {
            nameToEntry.put(getKey(entry), entry);
        }
    }
}
