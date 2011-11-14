package tlb.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @understands time talken to execute a test suite
 */
public class SuiteTimeEntry implements NamedEntry {
    private String name;
    private long time;
    public static final Pattern SUITE_TIME_STRING = Pattern.compile("(.*?):\\s*(\\d+)");

    public SuiteTimeEntry(String name, long time) {
        this.name = name;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public long getTime() {
        return time;
    }

    public static List<SuiteTimeEntry> parse(String buffer) {
        return parse(Arrays.asList(buffer.split("\n")));
    }

    public static String dump(List<SuiteTimeEntry> entries) {
        StringBuilder buffer = new StringBuilder();
        for (Entry entry : entries) {
            buffer.append(entry.dump());
        }
        return buffer.toString();
    }

    public String dump() {
        return toString() + "\n";
    }

    public static List<SuiteTimeEntry> parse(List<String> listOfStrings) {
        List<SuiteTimeEntry> parsed = new ArrayList<SuiteTimeEntry>();
        for (String entryString : listOfStrings) {
            if (entryString.trim().length() > 0) parsed.add(parseSingleEntry(entryString));
        }
        return parsed;
    }

    public static SuiteTimeEntry parseSingleEntry(String entryString) {
        Matcher matcher = SUITE_TIME_STRING.matcher(entryString);
        SuiteTimeEntry entry = null;
        if (matcher.matches()) {
            entry = new SuiteTimeEntry(matcher.group(1), Integer.parseInt(matcher.group(2)));
        } else {
            throw new IllegalArgumentException(String.format("failed to parse '%s' as %s", entryString, SuiteTimeEntry.class.getSimpleName()));
        }
        return entry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuiteTimeEntry that = (SuiteTimeEntry) o;

        if (time != that.time) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", name, time);
    }

    public SuiteTimeEntry smoothedWrt(SuiteTimeEntry newDataPoint, double alpha) {
        if ( ! name.equals(newDataPoint.name)) throw new IllegalArgumentException(String.format("suite %s can not be smoothed with data point from %s", name, newDataPoint.name));
        final double smoothedTime = alpha * newDataPoint.time + (1 - alpha) * time;
        return new SuiteTimeEntry(name, Math.round(smoothedTime));
    }
}
