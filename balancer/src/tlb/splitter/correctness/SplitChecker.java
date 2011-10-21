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

    public final List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> files) {
        universalSet(files);
        List<TlbSuiteFile> filteredFiles = splitter.filterSuites(files);
        subSet(filteredFiles);
        return filteredFiles;
    }

    abstract public void universalSet(List<TlbSuiteFile> fileResources);

    abstract public void subSet(List<TlbSuiteFile> fileResources);
}
