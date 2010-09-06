package com.github.tlb;

/**
 * @understands TBL constants
 */
public interface TlbConstants {
    static final String TALK_TO_SERVICE = "TALK_TO_SERVICE";

    public interface Cruise {
        static final String CRUISE_SERVER_URL = "CRUISE_SERVER_URL";
        static final String CRUISE_PIPELINE_NAME = "CRUISE_PIPELINE_NAME";
        static final String CRUISE_STAGE_NAME = "CRUISE_STAGE_NAME";
        static final String CRUISE_JOB_NAME = "CRUISE_JOB_NAME";
        static final String CRUISE_STAGE_COUNTER = "CRUISE_STAGE_COUNTER";
        static final String CRUISE_PIPELINE_COUNTER = "CRUISE_PIPELINE_COUNTER";
        static final String CRUISE_PIPELINE_LABEL = "CRUISE_PIPELINE_LABEL";
    }

    public interface TlbServer {
        static final String JOB_NAMESPACE = "TLB_JOB_NAME";
        static final String URL = "TLB_URL";
        static final String PARTITION_NUMBER = "PARTITION_NUMBER";
        static final String TOTAL_PARTITIONS = "TOTAL_PARTITIONS";
        static final String JOB_VERSION = "JOB_VERSION";
        static final String USE_SMOOTHING = "USE_SMOOTHING";
    }

    static final String PASSWORD = "TLB_PASSWORD";
    static final String USERNAME = "TLB_USERNAME";
    static final String TLB_CRITERIA = "TLB_CRITERIA";
    static final String TEST_SUBSET_SIZE_FILE = "tlb/subset_size";
    static final String CRITERIA_DEFAULTING_ORDER = "CRITERIA_DEFAULTING_ORDER";
    static final String TLB_TMP_DIR = "TLB_TMP_DIR";
    static final String TLB_ORDERER = "TLB_ORDERER";

    static final String TLB_APP = "TLB_APP";

    public interface Balancer {
        static final String TLB_BALANCER_PORT = "TLB_BALANCER_PORT";
        static final String QUERY = "query";
    }

    public interface Server {
        static final String REPO_FACTORY = "repo_factory";
        static final String REQUEST_NAMESPACE = "namespace";
        static final String TLB_PORT = "TLB_PORT";
        static final String TLB_STORE_DIR = "tlb_store";
        static final String LISTING_VERSION = "listing_version";
        static final String VERSION_LIFE_IN_DAYS = "VERSION_LIFE_IN_DAYS";
        static final String SMOOTHING_FACTOR = "SMOOTHING_FACTOR";
    }

}
