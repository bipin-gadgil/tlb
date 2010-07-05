package com.github.tlb.server.repo;

import com.github.tlb.domain.SuiteLevelEntry;

import java.io.IOException;

import static com.github.tlb.server.repo.EntryRepoFactory.LATEST_VERSION;

/**
 * @understands storage and retrival of test case to test suite mappings
 */
public class TestCaseRepo extends VersioningEntryRepo<TestCaseRepo.TestCaseEntry> {
    static class TestCaseEntry implements SuiteLevelEntry {
        private final String testName;
        private final String suiteName;

        TestCaseEntry(String testName, String suiteName) {
            this.testName = testName;
            this.suiteName = suiteName;
        }

        public String getName() {
            return testName;
        }

        public String dump() {
            return testName + "#" + suiteName;
        }

        @Override
        public String toString() {
            return dump();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestCaseEntry that = (TestCaseEntry) o;

            if (suiteName != null ? !suiteName.equals(that.suiteName) : that.suiteName != null) return false;
            if (testName != null ? !testName.equals(that.testName) : that.testName != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = testName != null ? testName.hashCode() : 0;
            result = 31 * result + (suiteName != null ? suiteName.hashCode() : 0);
            return result;
        }
    }

    @Override
    public TestCaseEntry getEntry(String record) {
        String[] nameAndHost = record.split("#");
        return new TestCaseEntry(nameAndHost[0], nameAndHost[1]);
    }

    @Override
    public TestCaseRepo getSubRepo(String versionIdentifier) throws IOException, ClassNotFoundException {
        return (TestCaseRepo) factory.findOrCreate(namespace, versionIdentifier, "test_case", new EntryRepoFactory.Creator<TestCaseRepo>() {
            public TestCaseRepo create() {
                return new TestCaseRepo();
            }
        });
    }

    public String getNamespace() {
        return namespace;
    }

    public EntryRepoFactory getFactory() {
        return factory;
    }
}