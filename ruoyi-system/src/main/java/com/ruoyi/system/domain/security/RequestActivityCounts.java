package com.ruoyi.system.domain.security;

/** 当前请求所需的访问频次统计。 */
public class RequestActivityCounts
{
    private int requestCount;
    private int repeatedQueryCount;

    public int getRequestCount()
    {
        return requestCount;
    }

    public void setRequestCount(int requestCount)
    {
        this.requestCount = requestCount;
    }

    public int getRepeatedQueryCount()
    {
        return repeatedQueryCount;
    }

    public void setRepeatedQueryCount(int repeatedQueryCount)
    {
        this.repeatedQueryCount = repeatedQueryCount;
    }
}
