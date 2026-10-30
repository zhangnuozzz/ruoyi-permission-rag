package com.ruoyi.common.utils.poi;

import java.util.List;
import java.util.function.Supplier;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.exception.CustomException;

/** 按完整筛选范围读取导出数据，超限明确拒绝。 */
public final class TableExportSupport
{
    public static final int MAX_ROWS = 10000;

    private TableExportSupport() {}

    public static <T> List<T> select(Supplier<List<T>> query)
    {
        try
        {
            PageHelper.startPage(1, MAX_ROWS + 1, false);
            List<T> rows = query.get();
            if (rows == null) throw new CustomException("导出数据读取失败，请重试");
            if (rows.size() > MAX_ROWS)
                throw new CustomException("单次最多导出10000条，请缩小筛选范围后重试");
            return rows;
        }
        finally
        {
            PageHelper.clearPage();
        }
    }
}
