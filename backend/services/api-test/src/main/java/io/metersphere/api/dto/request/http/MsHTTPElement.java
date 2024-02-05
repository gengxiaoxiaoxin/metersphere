package io.metersphere.api.dto.request.http;

import io.metersphere.api.dto.request.http.auth.HTTPAuth;
import io.metersphere.api.dto.request.http.body.Body;
import io.metersphere.plugin.api.spi.AbstractMsTestElement;
import io.metersphere.sdk.constants.HttpMethodConstants;
import io.metersphere.system.valid.EnumValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Http接口详情
 * <pre>
 * 其中包括：接口调试、接口定义、接口用例、场景的自定义请求 的详情
 * 接口协议插件的接口详情也类似
 * </pre>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MsHTTPElement extends AbstractMsTestElement {
    /**
     * 完整请求地址
     * 自定义请求时，使用该字段
     */
    @Size(max = 500)
    private String url;
    /**
     * 接口定义和用例的请求路径
     */
    @Size(max = 500)
    private String path;
    /**
     * 请求方法
     * 取值参考：{@link HttpMethodConstants}
     */
    @NotBlank
    @Size(max = 10)
    @EnumValue(enumClass = HttpMethodConstants.class)
    private String method;
    /**
     * 请求体
     */
    @Valid
    private Body body;
    /**
     * 请求头
     */
    @Valid
    private List<Header> headers;
    /**
     * rest参数
     */
    @Valid
    private List<RestParam> rest;
    /**
     * query参数
     */
    @Valid
    private List<QueryParam> query;
    /**
     * 其他配置
     */
    @Valid
    private MsHTTPConfig otherConfig;
    /**
     * 认证配置
     */
    @Valid
    private HTTPAuth authConfig;
}