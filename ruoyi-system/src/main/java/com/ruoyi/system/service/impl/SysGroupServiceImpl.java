package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.SysGroup;
import com.ruoyi.system.mapper.SysGroupMapper;
import com.ruoyi.system.service.ISysGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 维护知悉用户组，保留已有归属关系并保护被引用的组。
 */
@Service
public class SysGroupServiceImpl implements ISysGroupService
{
    @Autowired
    private SysGroupMapper sysGroupMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public SysGroup selectSysGroupById(Long id)
    {
        return sysGroupMapper.selectSysGroupById(id);
    }

    @Override
    public List<SysGroup> selectSysGroupList(SysGroup filter)
    {
        return sysGroupMapper.selectSysGroupList(filter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSysGroup(SysGroup input)
    {
        SysGroup group = validateInput(input);
        if (input.getId() != null)
        {
            throw new CustomException("新增用户组不能指定 ID");
        }
        if (!group.getGroupCode().matches("[A-Z][A-Z0-9_]{0,63}"))
        {
            throw new CustomException("用户组编码须以大写字母开头，仅使用大写字母、数字和下划线");
        }
        group.setDelFlag("0");
        group.setCreateBy(SecurityUtils.getUsername());
        group.setCreateTime(DateUtils.getNowDate());
        try
        {
            int rows = sysGroupMapper.insertSysGroup(group);
            requireSingleWrite(rows);
            input.setId(group.getId());
            return rows;
        }
        catch (DuplicateKeyException exception)
        {
            throw new CustomException("用户组编码已存在，请使用其他编码");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSysGroup(SysGroup input)
    {
        SysGroup group = validateInput(input);
        group.setId(requireId(input.getId()));
        Map<String, Object> current = lockGroup(group.getId());
        if (!group.getGroupCode().equals(current.get("group_code")))
        {
            throw new CustomException("用户组编码是文档归属标识，创建后不能修改");
        }
        group.setUpdateBy(SecurityUtils.getUsername());
        group.setUpdateTime(DateUtils.getNowDate());
        int rows = sysGroupMapper.updateSysGroup(group);
        requireSingleWrite(rows);
        return rows;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int deleteSysGroupByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new CustomException("请选择需要删除的用户组");
        }
        Set<Long> groupIds = new TreeSet<Long>();
        for (Long id : ids)
        {
            groupIds.add(requireId(id));
        }
        // 与成员、策略绑定及文档标签写入共用组行锁，整批核对后再删除。
        for (Long id : groupIds)
        {
            Map<String, Object> group = lockGroup(id);
            String code = (String) group.get("group_code");
            String name = (String) group.get("group_name");
            if ("GROUP_PUBLIC".equalsIgnoreCase(code))
            {
                throw new CustomException("公开兼容分组用于保留历史数据归属，不能删除");
            }
            requireNoReferences(id, code, name);
        }
        int rows = sysGroupMapper.deleteSysGroupByIds(groupIds.toArray(new Long[0]));
        if (rows != groupIds.size())
        {
            throw new CustomException("用户组未能全部删除，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int deleteSysGroupById(Long id)
    {
        return deleteSysGroupByIds(new Long[] { id });
    }

    private void requireNoReferences(Long id, String code, String name)
    {
        String prefix = "用户组“" + name + "”";
        // 使用读已提交隔离级别读取最新已提交引用，不反向锁成员或文档行。
        // 这避免与先锁人员或文档、再锁组的维护入口形成相反锁序。
        if (hasReferences("select exists(select 1 from sys_user_group_rel where group_id = ?)", id))
        {
            throw new CustomException(prefix + "仍有成员关联（含历史关系），请先移出成员");
        }
        if (hasReferences("select exists(select 1 from sys_policy_bind where bind_type = 'GROUP' "
                + "and bind_target_id = ?)", id))
        {
            throw new CustomException(prefix + "仍有策略绑定（含停用关联），请先取消绑定");
        }
        if (hasReferences("select exists(select 1 from sys_rag_doc where owner_group_code = ? "
                + "or doc_group_id = ?)", code, code))
        {
            throw new CustomException(prefix + "仍有文档标签引用（含停用或历史记录），请先处理文档归属");
        }
        if (hasReferences("select exists(select 1 from sys_rag_file where group_id = ? or doc_group = ?)",
                code, code))
        {
            throw new CustomException(prefix + "仍有文件元数据引用，请先处理文件归属");
        }
    }

    private boolean hasReferences(String sql, Object... values)
    {
        Long exists = jdbcTemplate.queryForObject(sql, Long.class, values);
        return exists == null || exists != 0;
    }

    private Map<String, Object> lockGroup(Long id)
    {
        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "select id, group_code, group_name from sys_group "
                + "where id = ? and del_flag = '0' for update", id);
        if (groups.size() != 1)
        {
            throw new CustomException("用户组不存在或已删除，请刷新后重试");
        }
        return groups.get(0);
    }

    private SysGroup validateInput(SysGroup input)
    {
        if (input == null)
        {
            throw new CustomException("用户组配置不能为空");
        }
        SysGroup group = new SysGroup();
        group.setGroupCode(requiredText(input.getGroupCode(), "用户组编码", 64));
        group.setGroupName(requiredText(input.getGroupName(), "用户组名称", 128));
        if (!"0".equals(input.getStatus()) && !"1".equals(input.getStatus()))
        {
            throw new CustomException("用户组状态必须为正常或停用");
        }
        group.setStatus(input.getStatus());
        String remark = input.getRemark() == null ? "" : input.getRemark().trim();
        if (remark.length() > 500)
        {
            throw new CustomException("说明不能超过 500 个字符");
        }
        group.setRemark(remark);
        // 不接收客户端的删除标记、旧版组密级及审计字段。
        return group;
    }

    private String requiredText(String value, String label, int maxLength)
    {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > maxLength)
        {
            throw new CustomException(label + "不能为空且不能超过 " + maxLength + " 个字符");
        }
        return text;
    }

    private Long requireId(Long id)
    {
        if (id == null || id <= 0)
        {
            throw new CustomException("用户组 ID 必须为正整数");
        }
        return id;
    }

    private void requireSingleWrite(int rows)
    {
        if (rows != 1)
        {
            throw new CustomException("用户组保存失败，请刷新后重试");
        }
    }
}
