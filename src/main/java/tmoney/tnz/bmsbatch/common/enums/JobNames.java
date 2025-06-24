package tmoney.tnz.bmsbatch.common.enums;

public enum JobNames {

    SAMPLE_JOB("sampleJob");

    private final String jobName;

    JobNames(String jobName) {
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }
}
