package com.ruoyi.common.utils.file;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.SecurityUtils;

/** 临时导出文件的归属登记、访问校验和过期清理。 */
@Component
@EnableScheduling
public class ExportFileStore
{
    private static final Logger log = LoggerFactory.getLogger(ExportFileStore.class);
    private static final String KEY_PREFIX = "export:file:owner:";
    private static final int EXPIRE_MINUTES = 30;
    private static final Pattern FILE_NAME = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_[\\p{L}\\p{N}_ .()-]+\\.xlsx");

    @Autowired
    private RedisCache redisCache;

    public void register(String fileName) throws IOException
    {
        String owner = currentOwner();
        Path file = resolve(fileName);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
        {
            throw new CustomException("导出文件未生成，请重新导出。", 500);
        }
        redisCache.setCacheObject(KEY_PREFIX + fileName, owner, EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    public Path authorize(String fileName) throws IOException
    {
        String owner = currentOwner();
        Path file = resolve(fileName);
        Object registeredOwner = redisCache.getCacheObject(KEY_PREFIX + fileName);
        if (!owner.equals(registeredOwner))
        {
            throw new CustomException("文件不存在、已过期或不属于当前用户，请重新导出。", 403);
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
        {
            throw new CustomException("导出文件已失效，请重新导出。", 404);
        }
        return file;
    }

    public void remove(String fileName) throws IOException
    {
        Path file = authorize(fileName);
        Files.deleteIfExists(file);
        redisCache.deleteObject(KEY_PREFIX + fileName);
    }

    private String currentOwner()
    {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || user.getUser() == null || user.getUser().getUserId() == null)
        {
            throw new CustomException("请先登录后再下载。", 401);
        }
        return String.valueOf(user.getUser().getUserId());
    }

    private Path resolve(String fileName) throws IOException
    {
        if (fileName == null || fileName.length() > 255 || !FILE_NAME.matcher(fileName).matches())
        {
            throw new CustomException("导出文件名无效。", 400);
        }
        Path root = Paths.get(RuoYiConfig.getDownloadPath()).toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root))
        {
            throw new CustomException("导出目录配置无效，请联系管理员。", 500);
        }
        Path file = root.resolve(fileName).normalize();
        if (!root.equals(file.getParent()) || Files.isSymbolicLink(file))
        {
            throw new CustomException("导出文件路径无效。", 400);
        }
        return file;
    }

    @Scheduled(fixedDelay = 600000, initialDelay = 600000)
    public void cleanExpiredFiles()
    {
        Path root = Paths.get(RuoYiConfig.getDownloadPath()).toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return;
        long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(EXPIRE_MINUTES + 1);
        try (DirectoryStream<Path> files = Files.newDirectoryStream(root, "*.xlsx"))
        {
            for (Path file : files)
            {
                String name = file.getFileName().toString();
                if (FILE_NAME.matcher(name).matches() && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                        && Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toMillis() < cutoff
                        && redisCache.getCacheObject(KEY_PREFIX + name) == null)
                {
                    Files.deleteIfExists(file);
                }
            }
        }
        catch (Exception exception)
        {
            log.warn("清理过期导出文件失败，将在下次任务重试", exception);
        }
    }
}
