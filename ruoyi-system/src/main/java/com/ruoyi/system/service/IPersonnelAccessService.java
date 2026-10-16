package com.ruoyi.system.service;

import java.util.Map;
import com.ruoyi.system.domain.PersonnelSaveRequest;

/**
 * 人员资料与访问配置管理。
 */
public interface IPersonnelAccessService
{
    Map<String, Object> getProfile(Long userId);

    void saveAccess(Long userId, Map<String, Object> access);

    Long createPersonnel(PersonnelSaveRequest request);

    void updatePersonnel(Long userId, PersonnelSaveRequest request);
}
