package tmoney.gbi.bmsbatch.common.enums;

public enum JobNames {

    SAMPLE_JOB("sampleJob") ,
    BMS_TXT_JOB("bmsTxtFile") ;

    private final String jobName;

    JobNames(String jobName) {
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }
}
