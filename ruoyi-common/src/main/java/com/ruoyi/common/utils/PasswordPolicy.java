package com.ruoyi.common.utils;

import com.ruoyi.common.exception.CustomException;

public final class PasswordPolicy
{
    private PasswordPolicy() { }

    public static void validate(String password)
    {
        if (password == null || !password.matches("[\\x21-\\x7E]{10,64}")
                || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*"))
        {
            throw new CustomException("密码须为10至64位，包含大写字母、小写字母和数字，可使用英文符号");
        }
    }
}
