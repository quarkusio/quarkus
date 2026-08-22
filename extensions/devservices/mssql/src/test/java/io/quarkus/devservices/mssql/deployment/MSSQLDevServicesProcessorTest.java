package io.quarkus.devservices.mssql.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MSSQLDevServicesProcessorTest {

    @ParameterizedTest
    @CsvSource({
            "1, 0-0",
            "2, 0-1",
            "3, 0-1",
            "7, 0-3",
            "8, 0-7",
            "9, 0-7",
            "15, 0-7",
            "16, 0-15"
    })
    void cpusetIsPowerOfTwoWithinAvailableCpus(int availableCpus, String expectedCpuset) {
        assertThat(MSSQLDevServicesProcessor.cpusetCpus(availableCpus)).isEqualTo(expectedCpuset);
    }

    @Test
    void cpusetNeverRequestsCpuIndexBeyondAvailableCount() {
        for (int available = 1; available <= 32; available++) {
            String cpuset = MSSQLDevServicesProcessor.cpusetCpus(available);
            int lastCpu = Integer.parseInt(cpuset.substring(cpuset.indexOf('-') + 1));
            assertThat(lastCpu)
                    .as("cpuset %s must fit in %s available CPUs", cpuset, available)
                    .isLessThan(available);
            assertThat(Integer.bitCount(lastCpu + 1)).isEqualTo(1);
        }
    }

    @Test
    void zeroOrNegativeCpuCountFallsBackToOneCpu() {
        assertThat(MSSQLDevServicesProcessor.cpusetCpus(0)).isEqualTo("0-0");
        assertThat(MSSQLDevServicesProcessor.cpusetCpus(-4)).isEqualTo("0-0");
    }
}
