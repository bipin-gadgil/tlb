package tlb.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @understands a single element of a Set of test-suites
 */
public class SuiteNameCountEntry implements SuiteLevelEntry {
    private final String name;
    private volatile short count;

    public SuiteNameCountEntry(String name) {
        this.name = name;
        this.count = 1;
    }

    public String getName() {
        return name;
    }

    public String dump() {
        return toString() + "\n";
    }

    public static List<SuiteNameCountEntry> parse(String suiteNamesString) {
        return parse(Arrays.asList(suiteNamesString.split("\n")));
    }

    public static List<SuiteNameCountEntry> parse(List<String> listOfStrings) {
        List<SuiteNameCountEntry> parsed = new ArrayList<SuiteNameCountEntry>();
        for (String entryString : listOfStrings) {
            if (entryString.trim().length() > 0) parsed.add(parseSingleEntry(entryString));
        }
        return parsed;
    }

    public static String dump(List<SuiteNameCountEntry> countEntries) {
        StringBuilder buffer = new StringBuilder();
        for (Entry entry : countEntries) {
            buffer.append(entry.dump());
        }
        return buffer.toString();
    }

    public static SuiteNameCountEntry parseSingleEntry(String singleEntryString) {
        return new SuiteNameCountEntry(singleEntryString.trim());
    }

    @Override
    public String toString() {
        return name;
    }

    public short getCount() {
        return count;
    }

    public synchronized boolean decrementCount() {
        return --count >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuiteNameCountEntry that = (SuiteNameCountEntry) o;

        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
