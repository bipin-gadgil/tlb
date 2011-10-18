package tlb.splitter.correctness;

import tlb.TlbSuiteFile;
import tlb.splitter.TestSplitter;

import java.util.List;

/**
 * @understands split correctness checking around balancer
 */
public abstract class SplitChecker implements TestSplitter {
    private final TestSplitter splitter;

    protected SplitChecker(final TestSplitter splitter) {
        this.splitter = splitter;
    }

    public List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources) {
        return splitter.filterSuites(fileResources);
    }
}
