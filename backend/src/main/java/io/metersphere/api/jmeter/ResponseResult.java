package io.metersphere.api.jmeter;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class ResponseResult {

    private String responseCode;

    private String responseMessage;

    private long responseTime;

    private long latency;

    private long responseSize;

    private String headers;

    private String body;

    private final List<ResponseAssertionResult> assertions = new ArrayList<>();

}
