package tlb.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SuiteNameCountEntryTest {

    //no point repeating count tests, randomly choose any entry, and assume others are the same

    @Test
    public void shouldParseItselfFromString() {
        String testTimesString = "com.thoughtworks.foo.FooBarTest\ncom.thoughtworks.hello.HelloWorldTest\ncom.thoughtworks.quux.QuuxTest";
        List<SuiteNameCountEntry> countEntries = SuiteNameCountEntry.parse(testTimesString);
        assertThat(countEntries.get(0).getName(), is("com.thoughtworks.foo.FooBarTest"));
        assertThat(countEntries.get(0).getCount(), is((short) 1));
        assertThat(countEntries.get(1).getName(), is("com.thoughtworks.hello.HelloWorldTest"));
        assertThat(countEntries.get(2).getName(), is("com.thoughtworks.quux.QuuxTest"));

    }

    @Test
    public void shouldParseItselfFromListOfStrings() {
        List<String> listOfStrings = Arrays.asList("com.thoughtworks.foo.FooBarTest", "com.thoughtworks.hello.HelloWorldTest", "com.thoughtworks.quux.QuuxTest");
        List<SuiteNameCountEntry> countEntries = SuiteNameCountEntry.parse(listOfStrings);
        assertThat(countEntries.get(0).getName(), is("com.thoughtworks.foo.FooBarTest"));
        assertThat(countEntries.get(1).getName(), is("com.thoughtworks.hello.HelloWorldTest"));
        assertThat(countEntries.get(1).getCount(), is((short) 1));
        assertThat(countEntries.get(2).getName(), is("com.thoughtworks.quux.QuuxTest"));
    }

    @Test
    public void shouldParseItselfFromListOfStringsInspiteOfEmptyStringsInBetween() {
        List<String> listOfStrings = Arrays.asList("com.thoughtworks.foo.FooBarTest", "", "com.thoughtworks.hello.HelloWorldTest", "", "com.thoughtworks.quux.QuuxTest");
        List<SuiteNameCountEntry> countEntries = SuiteNameCountEntry.parse(listOfStrings);

        assertThat(countEntries.size(), is(3));
        assertThat(countEntries.get(0).getName(), is("com.thoughtworks.foo.FooBarTest"));
        assertThat(countEntries.get(1).getName(), is("com.thoughtworks.hello.HelloWorldTest"));
        assertThat(countEntries.get(2).getName(), is("com.thoughtworks.quux.QuuxTest"));
        assertThat(countEntries.get(2).getCount(), is((short) 1));
    }

    @Test
    public void shouldDumpStringFromListOfEntries() {
        List<SuiteNameCountEntry> list = new ArrayList<SuiteNameCountEntry>();
        list.add(new SuiteNameCountEntry("com.thoughtworks.foo.FooBarTest"));
        list.add(new SuiteNameCountEntry("com.thoughtworks.hello.HelloWorldTest"));
        list.add(new SuiteNameCountEntry("com.thoughtworks.quux.QuuxTest"));
        assertThat(SuiteNameCountEntry.dump(list), is("com.thoughtworks.foo.FooBarTest\ncom.thoughtworks.hello.HelloWorldTest\ncom.thoughtworks.quux.QuuxTest\n"));
    }

    @Test
    public void shouldParseItselfFromSingleString() {
        SuiteNameCountEntry countEntry = SuiteNameCountEntry.parseSingleEntry("com.thoughtworks.foo.FooBarTest");
        assertThat(countEntry.getName(), is("com.thoughtworks.foo.FooBarTest"));
        assertThat(countEntry.getCount(), is((short) 1));
    }

    @Test
    public void shouldReturnDumpAsToString() {
        SuiteNameCountEntry countEntry = new SuiteNameCountEntry("foo.bar.Baz");
        assertThat(countEntry.toString(), is("foo.bar.Baz"));
    }
    
    @Test
    public void shouldDecrementCountAsUsed() {
        SuiteNameCountEntry countEntry = new SuiteNameCountEntry("foo.bar.Baz");
        assertThat(countEntry.getCount(), is((short) 1));
        assertThat(countEntry.decrementCount(), is(true));
        assertThat(countEntry.getCount(), is((short) 0));
        assertThat(countEntry.decrementCount(), is(false));
        assertThat(countEntry.getCount(), is((short) -1));
        assertThat(countEntry.decrementCount(), is(false));
        assertThat(countEntry.getCount(), is((short) -2));
    }
}
