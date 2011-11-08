package tlb.splitter.correctness;

import org.junit.Test;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.splitter.TestSplitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SplitCheckerTest {
    private static final String moduleName = "module_baz";

    @Test
    public void shouldExecuteBeforeAndAfterCallbackOnSplitCall() {
        TestSplitter splitter = mock(TestSplitter.class);

        final List<TlbSuiteFile> universalSet = new ArrayList<TlbSuiteFile>();
        final List<TlbSuiteFile> subSet = new ArrayList<TlbSuiteFile>();
        SplitChecker splitChecker = new SplitChecker(splitter) {
            @Override
            public void universalSet(List<TlbSuiteFile> fileResources) {
                for (TlbSuiteFile fileResource : fileResources) {
                    universalSet.add(fileResource);
                }
            }

            @Override
            public void subSet(List<TlbSuiteFile> fileResources) {
                for (TlbSuiteFile fileResource : fileResources) {
                    subSet.add(fileResource);
                }
            }
        };

        List<TlbSuiteFile> given = new ArrayList<TlbSuiteFile>();

        TlbSuiteFile foo = new TlbSuiteFileImpl("foo");
        given.add(foo);
        given.add(new TlbSuiteFileImpl("bar"));
        TlbSuiteFile baz = new TlbSuiteFileImpl("baz");
        given.add(baz);

        when(splitter.filterSuites(given, moduleName)).thenReturn(Arrays.asList(baz, foo));

        List<TlbSuiteFile> returned = splitChecker.filterSuites(given, moduleName);
        
        assertThat(universalSet, is(given));
        assertThat(subSet, is(Arrays.asList(baz, foo)));
        assertThat(returned, is(Arrays.asList(baz, foo)));
    }
}
