package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.SysRagDoc;
import com.ruoyi.system.mapper.SysRagDocMapper;
import com.ruoyi.system.service.ISysRagDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档权限标签维护，以平台标签作为访问判定依据。
 */
@Service
public class SysRagDocServiceImpl implements ISysRagDocService
{
    @Autowired
    private SysRagDocMapper sysRagDocMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public SysRagDoc selectSysRagDocById(Long id)
    {
        return sysRagDocMapper.selectSysRagDocById(id);
    }

    @Override
    public List<SysRagDoc> selectSysRagDocList(SysRagDoc filter)
    {
        return sysRagDocMapper.selectSysRagDocList(filter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSysRagDoc(SysRagDoc input)
    {
        requireInput(input);
        if (input.getId() != null)
        {
            throw new CustomException("新增文档标签不能指定 ID");
        }
        String docId = requiredText(input.getDocId(), 64, "文档编号");
        Map<String, Object> file = lockFile(docId);
        SysRagDoc document = writableFields(input, null);
        document.setDocId(docId);
        document.setUploadUserId(file == null ? SecurityUtils.getLoginUser().getUser().getUserId() : number(file.get("upload_user_id")));
        document.setUploadUserName(file == null ? SecurityUtils.getUsername() : text(file.get("upload_user_name")));
        document.setCreateBy(SecurityUtils.getUsername());
        document.setCreateTime(DateUtils.getNowDate());
        document.setDelFlag("0");
        try
        {
            requireSingleWrite(sysRagDocMapper.insertSysRagDoc(document));
        }
        catch (DuplicateKeyException exception)
        {
            throw new CustomException("文档编号已登记，请修改原有标签，避免重复创建");
        }
        syncFile(document, file);
        input.setId(document.getId());
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSysRagDoc(SysRagDoc input)
    {
        requireInput(input);
        if (input.getId() == null || input.getId() <= 0)
        {
            throw new CustomException("文档标签 ID 必须为正整数");
        }
        SysRagDoc original = sysRagDocMapper.selectSysRagDocById(input.getId());
        if (original == null)
        {
            throw new CustomException("文档标签不存在或已删除");
        }
        // 与上传登记使用相同锁序：文件、文档、知悉用户组。
        Map<String, Object> file = lockFile(original.getDocId());
        SysRagDoc current = sysRagDocMapper.selectSysRagDocByIdForUpdate(input.getId());
        if (current == null || !original.getDocId().equals(current.getDocId()))
        {
            throw new CustomException("文档归属已变化，请刷新后重试");
        }
        if (input.getDocId() != null && !current.getDocId().equals(input.getDocId().trim()))
        {
            throw new CustomException("文档编号是原文件关联标识，创建后不能修改");
        }
        if ((input.getUploadUserId() != null && !input.getUploadUserId().equals(current.getUploadUserId()))
                || (input.getUploadUserName() != null && !input.getUploadUserName().equals(current.getUploadUserName())))
        {
            throw new CustomException("上传人由原文件登记确定，不能在标签中修改");
        }
        SysRagDoc document = writableFields(input, current);
        document.setId(current.getId());
        document.setDocId(current.getDocId());
        document.setUpdateBy(SecurityUtils.getUsername());
        document.setUpdateTime(DateUtils.getNowDate());
        requireSingleWrite(sysRagDocMapper.updateSysRagDoc(document));
        syncFile(document, file);
        return 1;
    }

    @Override
    public int deleteSysRagDocByIds(Long[] ids)
    {
        return sysRagDocMapper.deleteSysRagDocByIds(ids);
    }

    @Override
    public int deleteSysRagDocById(Long id)
    {
        return sysRagDocMapper.deleteSysRagDocById(id);
    }

    private SysRagDoc writableFields(SysRagDoc input, SysRagDoc current)
    {
        SysRagDoc document = new SysRagDoc();
        document.setDocName(requiredText(input.getDocName() == null && current != null
                ? current.getDocName() : input.getDocName(), 200, "文档名称"));
        String level = code(input.getSecurityLevel() == null && current != null
                ? current.getSecurityLevel() : input.getSecurityLevel());
        if (!"PUBLIC".equals(level) && !"INTERNAL".equals(level)
                && !"SECRET".equals(level) && !"CONFIDENTIAL".equals(level))
        {
            throw new CustomException("请选择有效的文档密级，历史未知值需明确核对后修改");
        }
        String state = input.getMetadataStatus() == null
                ? (current == null ? "ACTIVE" : effectiveState(current)) : code(input.getMetadataStatus());
        if (!"ACTIVE".equals(state) && !"DISABLED".equals(state) && !"ARCHIVED".equals(state))
        {
            throw new CustomException("请选择有效的文档访问状态");
        }
        document.setSecurityLevel(level);
        document.setMetadataStatus(state);
        document.setStatus("ACTIVE".equals(state) ? "0" : "1");
        document.setOwnerGroupSecretLevel("");
        if ("PUBLIC".equals(level) || "INTERNAL".equals(level))
        {
            document.setScopeCode(level);
            document.setOwnerGroupCode("PUBLIC".equals(level) ? "GROUP_PUBLIC" : "");
            document.setOwnerGroupName("PUBLIC".equals(level) ? "公开文档组" : "");
        }
        else
        {
            String group = code(input.getOwnerGroupCode() == null && current != null
                    ? current.getOwnerGroupCode() : input.getOwnerGroupCode());
            if (group.isEmpty() || "GROUP_PUBLIC".equals(group))
            {
                throw new CustomException("秘密、机密文档必须选择唯一有效的知悉用户组");
            }
            List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                    "select group_code, group_name, status from sys_group "
                    + "where group_code = ? and del_flag = '0' for update", group);
            if (groups.size() != 1 || !"0".equals(text(groups.get(0).get("status"))))
            {
                throw new CustomException("文档绑定用户组不存在或已停用，请重新选择");
            }
            document.setOwnerGroupCode(text(groups.get(0).get("group_code")));
            document.setOwnerGroupName(text(groups.get(0).get("group_name")));
            document.setScopeCode(document.getOwnerGroupCode());
        }
        if (input.getRemark() != null && input.getRemark().length() > 500)
        {
            throw new CustomException("备注不能超过 500 个字符");
        }
        document.setRemark(input.getRemark());
        return document;
    }

    private Map<String, Object> lockFile(String docId)
    {
        List<Map<String, Object>> files = jdbcTemplate.queryForList(
                "select upload_user_id, upload_user_name, metadata_json from sys_rag_file where file_id = ? for update",
                docId);
        return files.isEmpty() ? null : files.get(0);
    }

    /** 只同步平台数据库；原文件、向量正文和模型服务不在此事务内。 */
    private void syncFile(SysRagDoc document, Map<String, Object> file)
    {
        if (file == null)
        {
            return;
        }
        JSONObject metadata;
        String raw = text(file.get("metadata_json"));
        try
        {
            metadata = raw.isEmpty() ? new JSONObject(true)
                    : JSON.parseObject(raw, com.alibaba.fastjson.parser.Feature.DisableSpecialKeyDetect);
            if (metadata == null)
            {
                throw new IllegalArgumentException("empty metadata object");
            }
        }
        catch (RuntimeException exception)
        {
            throw new CustomException("原文件元数据格式异常，本次修改已回滚，请联系管理员核对");
        }
        metadata.put("doc_level", document.getSecurityLevel());
        metadata.put("doc_group", document.getOwnerGroupCode());
        metadata.put("scope_code", document.getScopeCode());
        metadata.put("status", document.getMetadataStatus());
        requireSingleWrite(jdbcTemplate.update(
                "update sys_rag_file set security_level = ?, scope_code = ?, group_id = ?, group_name = ?, "
                + "doc_level = ?, doc_group = ?, doc_status = ?, metadata_json = ? where file_id = ?",
                document.getSecurityLevel(), document.getScopeCode(), document.getOwnerGroupCode(),
                document.getOwnerGroupName(), document.getSecurityLevel(), document.getOwnerGroupCode(),
                document.getMetadataStatus(), JSON.toJSONString(metadata), document.getDocId()));
    }

    private String effectiveState(SysRagDoc document)
    {
        if (document.getMetadataStatus() != null && !document.getMetadataStatus().isEmpty())
        {
            return code(document.getMetadataStatus());
        }
        return "0".equals(document.getStatus()) ? "ACTIVE" : "1".equals(document.getStatus()) ? "DISABLED" : "";
    }

    private void requireInput(SysRagDoc input)
    {
        if (input == null)
        {
            throw new CustomException("文档标签不能为空");
        }
    }

    private String requiredText(String value, int max, String label)
    {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty() || result.length() > max)
        {
            throw new CustomException(label + "不能为空且不能超过 " + max + " 个字符");
        }
        return result;
    }

    private String code(String value)
    {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String text(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    private Long number(Object value)
    {
        return value == null ? null : ((Number) value).longValue();
    }

    private void requireSingleWrite(int rows)
    {
        if (rows != 1)
        {
            throw new CustomException("文档元数据未能完整保存，请刷新后重试");
        }
    }
}
