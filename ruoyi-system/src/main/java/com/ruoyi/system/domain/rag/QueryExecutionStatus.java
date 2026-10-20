package com.ruoyi.system.domain.rag;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

public enum QueryExecutionStatus
{
    COMPLETED("已完成"),
    FAILED("执行失败"),
    NOT_EXECUTED("未执行"),
    UNKNOWN("未记录");

    private final String label;

    QueryExecutionStatus(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    public static QueryExecutionStatus from(String allowAccess, String responseJson)
    {
        if ("0".equals(allowAccess))
        {
            return NOT_EXECUTED;
        }
        if (!"1".equals(allowAccess) || responseJson == null || responseJson.trim().isEmpty())
        {
            return UNKNOWN;
        }
        try
        {
            JSONObject response = JSON.parseObject(responseJson, Feature.DisableSpecialKeyDetect);
            String code = response == null ? "" : String.valueOf(response.get("code"));
            if ("200".equals(code))
            {
                return COMPLETED;
            }
            return code.matches("[45][0-9]{2}") ? FAILED : UNKNOWN;
        }
        catch (RuntimeException exception)
        {
            return UNKNOWN;
        }
    }
}
