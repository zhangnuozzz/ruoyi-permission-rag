package com.ruoyi.common.core.domain.model;

public class PasswordChangeRequest
{
    private String oldPassword;
    private String newPassword;
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String value) { oldPassword = value; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String value) { newPassword = value; }
}
