package tlb.server.repo;

import org.apache.log4j.Logger;
import tlb.TlbConstants;
import tlb.domain.TimeProvider;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;
import org.apache.commons.io.FileUtils;

import java.io.*;

import static tlb.TlbConstants.Server.EntryRepoFactory.*;

/**
 * @understands creation of EntryRepo
 */
public class EntryRepoFactory implements Runnable {
    public static final String DELIMITER = "_";
    public static final String LATEST_VERSION = "LATEST";
    private static final Logger logger = Logger.getLogger(EntryRepoFactory.class.getName());

    //private final Map<String, EntryRepo> repos;
    private final String tlbStoreDir;
    private final TimeProvider timeProvider;
    private Cache<EntryRepo> cache;

    static interface Creator<T> {
        T create();
    }

    public EntryRepoFactory(SystemEnvironment env) {
        this(new File(env.val(TlbConstants.Server.TLB_DATA_DIR)), new TimeProvider());
    }

    EntryRepoFactory(File tlbStoreDir, TimeProvider timeProvider) {
        this.tlbStoreDir = tlbStoreDir.getAbsolutePath();
        this.cache = new Cache<EntryRepo>();
        this.timeProvider = timeProvider;
    }

    public void purge(String identifier) throws IOException {
        synchronized (mutex(identifier)) {
            cache.remove(identifier);
            File file = dumpFile(identifier);
            if (file.exists()) FileUtils.forceDelete(file);
        }
    }

    public static String mutex(String identifier) {
        return identifier.intern();
    }

    public void purgeVersionsOlderThan(int versionLifeInDays) {
        for (String identifier : cache.keys()) {
            EntryRepo entryRepo = cache.get(identifier);
            if (entryRepo instanceof VersioningEntryRepo) {
                final VersioningEntryRepo repo = (VersioningEntryRepo) entryRepo;
                try {
                    repo.purgeOldVersions(versionLifeInDays);
                } catch (Exception e) {
                    logger.warn(String.format("failed to delete older versions for repo identified by '%s'", identifier), e);
                }
            }
        }
    }

    public static abstract class IdentificationScheme {
        private final String type;

        protected IdentificationScheme(String type) {
            this.type = type;
        }

        public String getIdUnder(String namespace) {
            return escape(namespace) + DELIMITER + getIdWithoutNamespace() + DELIMITER + escape(type);
        }

        public abstract String getIdWithoutNamespace();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IdentificationScheme that = (IdentificationScheme) o;

            if (type != null ? !type.equals(that.type) : that.type != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return type != null ? type.hashCode() : 0;
        }
    }

    public static class VersionedNamespace extends IdentificationScheme {
        private final String version;

        public VersionedNamespace(String version, String type) {
            super(type);
            this.version = version;
        }

        @Override
        public String getIdWithoutNamespace() {
            return escape(version);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VersionedNamespace that = (VersionedNamespace) o;

            if (version != null ? !version.equals(that.version) : that.version != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return version != null ? version.hashCode() : 0;
        }
    }

    public static class SubmoduledUnderVersionedNamespace extends VersionedNamespace {
        private final String submoduleName;

        public SubmoduledUnderVersionedNamespace(String version, String type, String submoduleName) {
            super(version, type);
            this.submoduleName = submoduleName;
        }

        @Override
        public String getIdWithoutNamespace() {
            return super.getIdWithoutNamespace() + DELIMITER + escape(submoduleName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            SubmoduledUnderVersionedNamespace that = (SubmoduledUnderVersionedNamespace) o;

            if (submoduleName != null ? !submoduleName.equals(that.submoduleName) : that.submoduleName != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (submoduleName != null ? submoduleName.hashCode() : 0);
            return result;
        }
    }


    public SuiteResultRepo createSuiteResultRepo(final String namespace, final String version) throws ClassNotFoundException, IOException {
        return findOrCreate(namespace, new VersionedNamespace(version, SUITE_RESULT), new Creator<SuiteResultRepo>() {
            public SuiteResultRepo create() {
                return new SuiteResultRepo();
            }
        });
    }

    public SuiteTimeRepo createSuiteTimeRepo(final String namespace, final String version) throws IOException {
        return findOrCreate(namespace, new VersionedNamespace(version, SUITE_TIME), new Creator<SuiteTimeRepo>() {
            public SuiteTimeRepo create() {
                return new SuiteTimeRepo(timeProvider);
            }
        });
    }

    public SubsetSizeRepo createSubsetRepo(final String namespace, final String version) throws IOException {
        return findOrCreate(namespace, new VersionedNamespace(version, SUBSET_SIZE), new Creator<SubsetSizeRepo>() {
            public SubsetSizeRepo create() {
                return new SubsetSizeRepo();
            }
        });
    }

    public SetRepo createUniversalSetRepo(String namespace, String version, final String submoduleName) throws IOException {
        return findOrCreate(namespace, new SubmoduledUnderVersionedNamespace(version, UNIVERSAL_SET, submoduleName), new Creator<SetRepo>() {
            public SetRepo create() {
                return new SetRepo(timeProvider);
            }
        });
    }

    <T extends EntryRepo> T findOrCreate(String namespace, IdentificationScheme idScheme, Creator<T> creator) throws IOException {
        String identifier = idScheme.getIdUnder(namespace);
        T repo = (T) cache.get(identifier);
        if (repo == null) {
            synchronized (mutex(identifier)) {
                repo = (T) cache.get(identifier);
                if (repo == null) {
                    repo = creator.create();
                    repo.setNamespace(namespace);
                    repo.setIdentifier(identifier);
                    cache.put(identifier, repo);

                    File diskDump = dumpFile(identifier);
                    if (diskDump.exists()) {
                        final FileReader reader = new FileReader(diskDump);
                        repo.loadCopyFromDisk(FileUtil.readIntoString(new BufferedReader(reader)));
                    }
                }
            }
        }
        if (! repo.hasFactory()) {
            synchronized (mutex(identifier)) {
                if (! repo.hasFactory()) {
                    repo.setFactory(this);
                }
            }
        }
        return repo;
    }

    private File dumpFile(String identifier) {
        new File(tlbStoreDir).mkdirs();
        return new File(tlbStoreDir, identifier);
    }

    private static String escape(String str) {
        return str.replace(DELIMITER, DELIMITER + DELIMITER);
    }

    @Deprecated //for tests only
    Cache<EntryRepo> getRepos() {
        return cache;
    }

    public void run() {
        syncReposToDisk();
    }

    public void syncReposToDisk() {
        for (String identifier : cache.keys()) {
            FileWriter writer = null;
            try {
                //don't care about a couple entries not being persisted(at teardown), as client is capable of balancing on averages(treat like new suites)
                EntryRepo entryRepo = cache.get(identifier);
                if (entryRepo != null) {
                    synchronized (mutex(identifier)) {
                        entryRepo = cache.get(identifier);
                        if (entryRepo != null && entryRepo.isDirty()) {
                            writer = new FileWriter(dumpFile(identifier));
                            String dump = entryRepo.diskDump();
                            writer.write(dump);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn(String.format("disk dump of %s failed, tlb server may not be able to perform data dependent operations well on next reboot.", identifier), e);
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    logger.warn(String.format("closing of disk dump file of %s failed, tlb server may not be able to perform data dependent operations well on next reboot.", identifier), e);
                }
            }
        }
    }

    public void registerExitHook() {
        Runtime.getRuntime().addShutdownHook(exitHook());
    }

    public Thread exitHook() {
        return new Thread(this);
    }
}
