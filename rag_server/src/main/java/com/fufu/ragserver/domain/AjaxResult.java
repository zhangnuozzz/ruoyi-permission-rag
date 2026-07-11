package com.fufu.ragserver.domain;

import java.util.HashMap;

/**
 * 若依兼容响应对象
 *
 * @author fufu
 * @date 2026-05-12
 */
public class AjaxResult extends HashMap<String, Object>
{
    private static final long serialVersionUID = 1L;
    public static final String CODE_TAG = "code";
    public static final String MSG_TAG = "msg";
    public static final String DATA_TAG = "data";

    public AjaxResult(int code, String msg)
    {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    public AjaxResult(int code, String msg, Object data)
    {
        this(code, msg);
        if (data != null)
        {
            super.put(DATA_TAG, data);
        }
    }

    public static AjaxResult success(Object data)
    {
        return new AjaxResult(200, "操作成功", data);
    }

    public static AjaxResult error(String msg)
    {
        return new AjaxResult(500, msg);
    }
}
