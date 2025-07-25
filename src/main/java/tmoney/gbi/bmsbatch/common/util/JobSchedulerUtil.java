package tmoney.gbi.bmsbatch.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tmoney.gbi.bmsbatch.common.enums.JobNames;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JobSchedulerUtil {

    public static String currentMethodName() {
        return Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public static String getJobName(JobNames jobNames) {
        return jobNames.getJobName();
    }

}
