package tlb.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @understands a single element of a Set of test-suites
 */
public class SuiteNameCountEntry implements SuiteLevelEntry {
    public static final Pattern SUITE_SET_ENTRY_STRING = Pattern.compile("(.*?)(:\\s*(\\d+)/(\\d+))?");

    private final String name;
    private PartitionIdentifier partitionIdentifier;

    public SuiteNameCountEntry(String name) {
        this(name, null);
    }

    public SuiteNameCountEntry(String name, PartitionIdentifier partitionIdentifier) {
        this.name = name;
        this.partitionIdentifier = partitionIdentifier;
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
        Matcher matcher = SUITE_SET_ENTRY_STRING.matcher(singleEntryString);
        if (matcher.matches()) {
            PartitionIdentifier partitionIdentifier = null;
            if (matcher.group(2) != null) {
                partitionIdentifier = new PartitionIdentifier(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
            }
            return new SuiteNameCountEntry(matcher.group(1), partitionIdentifier);
        } else {
            throw new IllegalArgumentException(String.format("failed to parse '%s' as %s", singleEntryString, SuiteNameCountEntry.class.getSimpleName()));
        }
    }

    @Override
    public String toString() {
        return isUsedByAnyPartition() ? String.format("%s: %s", name, partitionIdentifier) : name;

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

    public boolean isUsedByPartitionOtherThan(PartitionIdentifier partitionIdentifier) {
        return isUsedByAnyPartition() && !this.partitionIdentifier.equals(partitionIdentifier);
    }

    public boolean isUsedByAnyPartition() {
        return this.partitionIdentifier != null;
    }

    public void markUsedBy(final PartitionIdentifier partitionIdentifier) {
        this.partitionIdentifier = partitionIdentifier;
    }

    public PartitionIdentifier getPartitionIdentifier() {
        return partitionIdentifier;
    }

    public static class SuiteNameCountEntryComparator implements Comparator<SuiteNameCountEntry> {
        public int compare(SuiteNameCountEntry o1, SuiteNameCountEntry o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static final class PartitionIdentifier {
        public final int partitionNumber;
        public final int totalPartitions;

        public PartitionIdentifier(int partitionNumber, int totalPartitions) {
            this.partitionNumber = partitionNumber;
            this.totalPartitions = totalPartitions;
        }

        @Override
        public String toString() {
            return String.format("%s/%s", partitionNumber, totalPartitions);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PartitionIdentifier that = (PartitionIdentifier) o;

            if (partitionNumber != that.partitionNumber) return false;
            if (totalPartitions != that.totalPartitions) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = partitionNumber;
            result = 31 * result + totalPartitions;
            return result;
        }
    }
}
