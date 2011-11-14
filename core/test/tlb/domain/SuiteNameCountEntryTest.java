package tlb.domain;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SuiteNameCountEntryTest {
    public static void assertNotInUse(SuiteNameCountEntry entry, final String name) {
        assertThat(entry.getName(), CoreMatchers.is(name));
        assertThat(entry.isUsedByAnyPartition(), CoreMatchers.is(false));
    }

    public static void assertInUse(SuiteNameCountEntry entry, final String name, int partitionNumber, int otherPartitionNumber, final int totalPartitions) {
        assertThat(entry.getName(), CoreMatchers.is(name));
        assertThat(entry.isUsedByAnyPartition(), CoreMatchers.is(true));
        assertThat(entry.isUsedByPartitionOtherThan(new PartitionIdentifier(partitionNumber, totalPartitions)), CoreMatchers.is(false));
        assertThat(entry.isUsedByPartitionOtherThan(new PartitionIdentifier(otherPartitionNumber, totalPartitions)), CoreMatchers.is(true));
    }

    //no point repeating count tests, randomly choose any entry, and assume others are the same

    @Test
    public void shouldParseItselfFromString() {
        String testTimesString = "com.thoughtworks.foo.FooBarTest\ncom.thoughtworks.hello.HelloWorldTest:1/3\ncom.thoughtworks.quux.QuuxTest:2/3";
        List<SuiteNameCountEntry> countEntries = SuiteNameCountEntry.parse(testTimesString);
        assertNotInUse(countEntries.get(0), "com.thoughtworks.foo.FooBarTest");
        assertInUse(countEntries.get(1), "com.thoughtworks.hello.HelloWorldTest", 1, 2, 3);
        assertInUse(countEntries.get(2), "com.thoughtworks.quux.QuuxTest", 2, 3, 3);
    }

    @Test
    public void shouldParseItselfFromListOfStrings() {
        List<String> listOfStrings = Arrays.asList("com.thoughtworks.foo.FooBarTest", "com.thoughtworks.hello.HelloWorldTest: 1/5", "com.thoughtworks.quux.QuuxTest");
        List<SuiteNameCountEntry> countEntries = SuiteNameCountEntry.parse(listOfStrings);
        assertNotInUse(countEntries.get(0), "com.thoughtworks.foo.FooBarTest");
        assertInUse(countEntries.get(1), "com.thoughtworks.hello.HelloWorldTest", 1, 4, 5);
        assertNotInUse(countEntries.get(2), "com.thoughtworks.quux.QuuxTest");
    }

    @Test
    public void shouldParseItselfFromListOfStringsInspiteOfEmptyStringsInBetween() {
        List<String> listOfStrings = Arrays.asList("com.thoughtworks.foo.FooBarTest: 2/3", "", "com.thoughtworks.hello.HelloWorldTest: 3/3", "", "com.thoughtworks.quux.QuuxTest");
        List<SuiteNameCountEntry> countEntries = SuiteNameCountEntry.parse(listOfStrings);

        assertThat(countEntries.size(), is(3));
        assertInUse(countEntries.get(0), "com.thoughtworks.foo.FooBarTest", 2, 1, 3);
        assertInUse(countEntries.get(1), "com.thoughtworks.hello.HelloWorldTest", 3, 1, 3);
        assertNotInUse(countEntries.get(2), "com.thoughtworks.quux.QuuxTest");
    }

    @Test
    public void shouldDumpStringFromListOfEntries() {
        List<SuiteNameCountEntry> list = new ArrayList<SuiteNameCountEntry>();
        list.add(new SuiteNameCountEntry("com.thoughtworks.foo.FooBarTest"));
        SuiteNameCountEntry usedEntry = new SuiteNameCountEntry("com.thoughtworks.hello.HelloWorldTest");
        usedEntry.markUsedBy(new PartitionIdentifier(2, 4));
        list.add(usedEntry);
        list.add(new SuiteNameCountEntry("com.thoughtworks.quux.QuuxTest"));
        assertThat(SuiteNameCountEntry.dump(list), is("com.thoughtworks.foo.FooBarTest\ncom.thoughtworks.hello.HelloWorldTest: 2/4\ncom.thoughtworks.quux.QuuxTest\n"));
    }

    @Test
    public void shouldParseItselfFromSingleString() {
        SuiteNameCountEntry countEntry = SuiteNameCountEntry.parseSingleEntry("com.thoughtworks.foo.FooBarTest");
        assertNotInUse(countEntry, "com.thoughtworks.foo.FooBarTest");

        countEntry = SuiteNameCountEntry.parseSingleEntry("com.thoughtworks.foo.FooBazTest: 2/3");
        assertInUse(countEntry, "com.thoughtworks.foo.FooBazTest", 2, 1, 3);

        countEntry = SuiteNameCountEntry.parseSingleEntry("com.thoughtworks.foo.FooQuuxTest:1/5");
        assertInUse(countEntry, "com.thoughtworks.foo.FooQuuxTest", 1, 2, 5);
    }

    @Test
    public void shouldReturnDumpAsToString() {
        SuiteNameCountEntry countEntry = new SuiteNameCountEntry("foo.bar.Baz");
        assertThat(countEntry.toString(), is("foo.bar.Baz"));

        countEntry = new SuiteNameCountEntry("foo.bar.Quux");
        countEntry.markUsedBy(new PartitionIdentifier(1, 2));
        assertThat(countEntry.toString(), is("foo.bar.Quux: 1/2"));
    }
}
