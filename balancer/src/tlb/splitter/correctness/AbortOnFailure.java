package tlb.splitter.correctness;

import tlb.splitter.TestSplitter;

/**
 * @understands aborting when split check fails
 */
public class AbortOnFailure extends SplitChecker {

    public AbortOnFailure(final TestSplitter splitter) {
        super(splitter);
    }
}
