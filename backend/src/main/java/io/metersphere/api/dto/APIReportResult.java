package io.metersphere.api.dto;

import io.metersphere.base.domain.ApiTestReport;
import io.metersphere.base.domain.ApiTestWithBLOBs;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class APIReportResult extends ApiTestReport {

    private String projectName;
}
