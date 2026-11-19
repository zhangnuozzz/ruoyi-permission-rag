package com.ruoyi.common.utils;

import java.util.regex.Pattern;
import com.ruoyi.common.exception.CustomException;

public final class PasswordPolicy
{
    private static final Pattern PRINTABLE_ASCII = Pattern.compile("[!-~]{10,64}");
    private static final Pattern[] CHARACTER_TYPES = {
        Pattern.compile("[A-Z]"), Pattern.compile("[a-z]"),
        Pattern.compile("[0-9]"), Pattern.compile("[^A-Za-z0-9]")
    };

    private PasswordPolicy() { }

    public static void validate(String password)
    {
        if (password == null || !PRINTABLE_ASCII.matcher(password).matches())
        {
            throw new CustomException("密码须为10至64位，使用英文字母、数字或英文符号");
        }
        int types = 0;
        for (Pattern type : CHARACTER_TYPES)
        {
            if (type.matcher(password).find() && ++types == 2)
            {
                return;
            }
        }
        throw new CustomException("密码须包含大写字母、小写字母、数字和英文符号中的至少两类");
    }
}
