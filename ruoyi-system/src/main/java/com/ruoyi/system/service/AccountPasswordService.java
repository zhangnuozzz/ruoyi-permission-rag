package com.ruoyi.system.service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.PasswordPolicy;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountPasswordService
{
    @Autowired
    private JdbcTemplate jdbc;

    @Value("${account.password.maxAgeDays:90}")
    private int maxAgeDays;

    private final Clock clock;

    public AccountPasswordService() { this(Clock.systemUTC()); }

    AccountPasswordService(Clock clock) { this.clock = clock; }

    public boolean requiresChange(Long userId)
    {
        int days = validMaxAgeDays();
        List<Boolean> rows = jdbc.query(
                "select changed_at, change_required from sys_password_state where user_id = ?",
                (rs, row) -> {
                    Timestamp changed = rs.getTimestamp("changed_at");
                    return rs.getInt("change_required") != 0 || changed == null
                            || !changed.toInstant().plus(Duration.ofDays(days)).isAfter(clock.instant());
                }, userId);
        return rows.size() != 1 || rows.get(0);
    }

    public int validMaxAgeDays()
    {
        if (maxAgeDays < 1 || maxAgeDays > 365) throw new IllegalStateException("密码有效期配置须为1至365天");
        return maxAgeDays;
    }

    public String encodeInitial(String password)
    {
        PasswordPolicy.validate(password);
        return SecurityUtils.encryptPassword(password);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(Long userId, String encodedPassword)
    {
        jdbc.update("insert into sys_password_state (user_id, changed_at, change_required) values (?, ?, 0)",
                userId, Timestamp.from(clock.instant()));
        remember(userId, encodedPassword);
    }

    @Transactional(rollbackFor = Exception.class)
    public String change(Long userId, String oldPassword, String newPassword, boolean administrativeReset)
    {
        PasswordPolicy.validate(newPassword);
        List<Map<String, Object>> users = jdbc.queryForList(
                "select password from sys_user where user_id = ? and del_flag = '0' for update", userId);
        if (users.size() != 1) throw new CustomException("账号不存在或已删除");
        String current = (String) users.get(0).get("password");
        if (!administrativeReset && (oldPassword == null || !SecurityUtils.matchesPassword(oldPassword, current)))
            throw new CustomException("当前密码错误");
        if (SecurityUtils.matchesPassword(newPassword, current)) throw new CustomException("新密码不能与当前或曾用密码相同");
        List<String> history = jdbc.queryForList(
                "select password_hash from sys_password_history where user_id = ?", String.class, userId);
        for (String previous : history)
        {
            if (SecurityUtils.matchesPassword(newPassword, previous))
                throw new CustomException("新密码不能与当前或曾用密码相同");
        }
        remember(userId, current);
        String encoded = SecurityUtils.encryptPassword(newPassword);
        if (jdbc.update("update sys_user set password = ?, update_time = now() where user_id = ? and del_flag = '0'",
                encoded, userId) != 1) throw new CustomException("密码保存失败");
        jdbc.update("insert into sys_password_state (user_id, changed_at, change_required) values (?, ?, ?) "
                + "on duplicate key update changed_at = values(changed_at), change_required = values(change_required)",
                userId, Timestamp.from(clock.instant()), administrativeReset ? 1 : 0);
        remember(userId, encoded);
        return encoded;
    }

    private void remember(Long userId, String encoded)
    {
        jdbc.update("insert into sys_password_history (user_id, password_hash, changed_at) "
                + "select ?, ?, ? where not exists (select 1 from sys_password_history where user_id = ? and password_hash = ?)",
                userId, encoded, Timestamp.from(clock.instant()), userId, encoded);
    }
}
