package tlb.splitter.correctness;

import tlb.TlbSuiteFile;
import tlb.splitter.TestSplitter;
import tlb.utils.SystemEnvironment;

import java.util.List;

/**
 * @understands split-check functionality disabled state
 */
public class NoOp extends SplitChecker {
    public NoOp(TestSplitter splitter) {
        super(splitter);
    }
}
