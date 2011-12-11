package tlb.server.repo;

import tlb.domain.Entry;

import java.io.*;
import java.util.Collection;
import java.util.List;

/**
 * @understands storage and retrieval of records 
 */
public interface EntryRepo<T extends Entry> extends Serializable {
    Collection<T> list();

    void update(T entry);

    String diskDump();

    String dump();

    void loadCopyFromDisk(final String fileContents);

    void load(final String contents);

    void add(T entry);

    void setFactory(EntryRepoFactory factory);

    boolean hasFactory();

    void setNamespace(String namespace);

    void setIdentifier(String type);

    String getIdentifier();

    List<T> parse(String string);

    boolean isDirty();
}
