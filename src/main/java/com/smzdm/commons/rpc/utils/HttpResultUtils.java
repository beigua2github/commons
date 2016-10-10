package com.smzdm.commons.rpc.utils;

import com.smzdm.commons.rpc.GenericHttp;
import com.smzdm.commons.rpc.entity.HttpInvokerMethod;
import com.smzdm.commons.rpc.entity.HttpResult;
import com.smzdm.commons.rpc.enums.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Created by zhengwenzhu on 16/10/10.
 */
public final class HttpResultUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpResultUtils.class);
    private static final Type resultMapKeyType = (new TypeReference() {
    }).getType();

    public HttpResultUtils() {
    }

    public static HttpResult generateHttpResult(GenericHttp realWorker, HttpInvokerMethod invokerMethod, Object[] args) {
        int retryTimes = invokerMethod.getRetryTimes();
        String errorMessage = "";
        String responseBody = "";

        do {
            try {
                if(invokerMethod.getMethod() == HttpMethod.GET) {
                    responseBody = realWorker.doGet(invokerMethod.getUrl(), invokerMethod.getParams());
                } else if(invokerMethod.getMethod() == HttpMethod.POST) {
                    responseBody = realWorker.doPost(invokerMethod.getUrl(), invokerMethod.getParams());
                } else {
                    if(invokerMethod.getMethod() != HttpMethod.POST_FILE) {
                        throw new UnsupportedOperationException("Unsupported HttpMethod: " + invokerMethod.getMethod().name());
                    }

                    responseBody = realWorker.doPostFile(invokerMethod.getUrl(), (InputStream)args[0]);
                }
            } catch (Exception var9) {
                logger.error("generateHttpResult error, retryTimes:" + retryTimes + ", " + var9.getMessage(), var9);
                errorMessage = var9.getMessage();
                --retryTimes;
                continue;
            }

            try {
                if(StringUtils.isEmpty(invokerMethod.getResultMapKey())) {
                    return realWorker.processResponse(responseBody, invokerMethod.getHttpInvokerMethodResult().getReturnType());
                }

                HttpResult e = realWorker.processResponse(responseBody, resultMapKeyType);
                if(!e.isSuccess()) {
                    return e;
                }

                if(e.getData() != null) {
                    String resultMapValue = (String)((Map)e.getData()).get(invokerMethod.getResultMapKey());
                    return HttpResult.success(parseResultMapValue(resultMapValue, invokerMethod.getHttpInvokerMethodResult().getReturnType()));
                }

                return HttpResult.success((Object)null);
            } catch (Exception var8) {
                errorMessage = "parse json error, responseBody: " + responseBody + ",errorMessage: " + var8.getMessage() + ",url: " + invokerMethod.getUrl() + ",params: " + invokerMethod.getParams();
                logger.error("generateHttpResult error, retryTimes:" + retryTimes + ", " + errorMessage, var8);
                --retryTimes;
            }
        } while(retryTimes >= 0);

        return HttpResult.fail(errorMessage);
    }

    private static Object parseResultMapValue(String resultMapValue, Type returnType) {
        return StringUtils.isEmpty(resultMapValue)?null:(String.class == returnType?resultMapValue:(returnType == Integer.class?Integer.valueOf(Integer.parseInt(resultMapValue)):(returnType == Boolean.class?Boolean.valueOf(Boolean.parseBoolean(resultMapValue)):(returnType == Long.class?Long.valueOf(Long.parseLong(resultMapValue)):(returnType == Float.class?Float.valueOf(Float.parseFloat(resultMapValue)):(returnType == Double.class?Double.valueOf(Double.parseDouble(resultMapValue)):(returnType == Byte.class?Byte.valueOf(Byte.parseByte(resultMapValue)):(returnType == Short.class?Short.valueOf(Short.parseShort(resultMapValue)):(returnType == Character.class?Character.valueOf(resultMapValue.charAt(0)):JSON.parseObject(resultMapValue, returnType, new Feature[0]))))))))));
    }
}