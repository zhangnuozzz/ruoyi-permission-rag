package com.ruoyi.system.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.SysRagBehaviorAlert;

/** 将告警摘要提交到独立、有界的发送队列，网络异常不阻塞业务请求。 */
@Service
public class SyslogForwardService
{
    private static final Logger log = LoggerFactory.getLogger(SyslogForwardService.class);

    @Value("${vacp.syslog.enabled:false}")
    private boolean enabled;
    @Value("${vacp.syslog.host:127.0.0.1}")
    private String host;
    @Value("${vacp.syslog.port:5514}")
    private int port;
    @Value("${vacp.syslog.protocol:UDP}")
    private String protocol;
    @Value("${vacp.syslog.queue-capacity:256}")
    private int queueCapacity;
    @Value("${vacp.syslog.timeout-ms:1000}")
    private int timeoutMillis;
    @Value("${vacp.syslog.include-query:false}")
    private boolean includeQuery;

    private volatile ThreadPoolExecutor executor;
    private volatile String state = "NOT_STARTED";
    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong sent = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong lastWarning = new AtomicLong();
    private volatile Long lastSentAt;
    private volatile Long lastFailureAt;

    @PostConstruct
    public synchronized void initialize()
    {
        if (!"NOT_STARTED".equals(state)) return;
        if (!enabled)
        {
            state = "DISABLED";
            return;
        }
        protocol = protocol == null ? "" : protocol.trim().toUpperCase(Locale.ROOT);
        host = host == null ? "" : host.trim();
        if (host.isEmpty() || port < 1 || port > 65535 || (!"UDP".equals(protocol) && !"TCP".equals(protocol))
                || queueCapacity < 1 || queueCapacity > 10000 || timeoutMillis < 100 || timeoutMillis > 10000)
        {
            state = "INVALID_CONFIGURATION";
            log.warn("Syslog 转发配置无效，转发已停止，平台业务继续运行");
            return;
        }
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), task -> {
                    Thread thread = new Thread(task, "syslog-forward");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        state = "ACTIVE";
    }

    /** 返回值表示已提交发送队列，不表示接收端已经收到或持久化。 */
    public boolean forwardAlert(SysRagBehaviorAlert alert)
    {
        if (alert == null || !enabled) return false;
        ThreadPoolExecutor sender = executor;
        if (!"ACTIVE".equals(state) || sender == null)
        {
            rejected.incrementAndGet();
            warnFailure("NOT_ACTIVE");
            return false;
        }
        // 生成不可变消息快照，避免排队期间告警对象被后续处理动作改变。
        final String message = buildMessage(alert);
        submitted.incrementAndGet();
        try
        {
            sender.execute(() -> {
                try
                {
                    if ("TCP".equals(protocol)) sendTcp(message);
                    else sendUdp(message);
                    sent.incrementAndGet();
                    lastSentAt = System.currentTimeMillis();
                }
                catch (Exception exception)
                {
                    failed.incrementAndGet();
                    warnFailure(exception.getClass().getSimpleName());
                }
            });
            return true;
        }
        catch (RejectedExecutionException exception)
        {
            submitted.decrementAndGet();
            rejected.incrementAndGet();
            warnFailure("QUEUE_FULL_OR_STOPPED");
            return false;
        }
    }

    /** 本进程累计计数；sent 仅表示本机发送完成，不包含接收端确认。 */
    public Map<String, Object> getStatus()
    {
        ThreadPoolExecutor sender = executor;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("state", state);
        result.put("protocol", protocol);
        result.put("submitted", submitted.get());
        result.put("sent", sent.get());
        result.put("failed", failed.get());
        result.put("rejected", rejected.get());
        result.put("pending", sender == null ? 0 : sender.getQueue().size() + sender.getActiveCount());
        result.put("lastSentAt", lastSentAt);
        result.put("lastFailureAt", lastFailureAt);
        result.put("includeQuery", includeQuery);
        result.put("receiverAcknowledged", false);
        return result;
    }

    @PreDestroy
    public synchronized void shutdown()
    {
        state = "STOPPED";
        ThreadPoolExecutor sender = executor;
        if (sender == null) return;
        sender.shutdown();
        try
        {
            if (!sender.awaitTermination(1500, TimeUnit.MILLISECONDS))
            {
                rejected.addAndGet(sender.shutdownNow().size());
            }
        }
        catch (InterruptedException exception)
        {
            rejected.addAndGet(sender.shutdownNow().size());
            Thread.currentThread().interrupt();
        }
    }

    private String buildMessage(SysRagBehaviorAlert alert)
    {
        Date occurredAt = alert.getCreateTime() == null ? new Date() : alert.getCreateTime();
        String timestamp = new SimpleDateFormat("MMM dd HH:mm:ss", Locale.ENGLISH).format(occurredAt);
        // local0 的 facility 为 16，严重程度按告警等级映射。
        int severity = "critical".equalsIgnoreCase(alert.getAlertLevel()) ? 2
                : "high".equalsIgnoreCase(alert.getAlertLevel()) ? 3
                : "low".equalsIgnoreCase(alert.getAlertLevel()) ? 6 : 4;
        String base = "<" + (16 * 8 + severity) + ">" + timestamp + " vacp-platform VACP-RAG-AUDIT:"
                + " alert_id=" + alert.getId() + " source_log_id=" + alert.getSourceLogId()
                + " user_id=" + alert.getUserId() + " user_name=\"" + escape(alert.getUserName(), 64) + "\""
                + " alert_type=" + token(alert.getAlertType()) + " alert_level=" + token(alert.getAlertLevel())
                + " status=" + token(alert.getStatus()) + " allow_access=" + token(alert.getAllowAccess())
                + " cost_time=" + alert.getCostTime();
        if (!includeQuery) return base;
        String details = base + " query=\"" + escape(alert.getQueryText(), 256) + "\""
                + " reason=\"" + escape(alert.getAlertReason(), 256) + "\"";
        if (details.getBytes(StandardCharsets.UTF_8).length > 1024) return base + " details_omitted=true";
        if (codePoints(alert.getQueryText()) > 256 || codePoints(alert.getAlertReason()) > 256)
        {
            details += " details_truncated=true";
            if (details.getBytes(StandardCharsets.UTF_8).length > 1024) return base + " details_omitted=true";
        }
        return details;
    }

    private int codePoints(String value)
    {
        return value == null ? 0 : value.codePointCount(0, value.length());
    }

    private String token(String value)
    {
        return escape(value, 64).replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private String escape(String value, int limit)
    {
        if (value == null) return "";
        int end = value.offsetByCodePoints(0, Math.min(limit, codePoints(value)));
        return value.substring(0, end).replaceAll("[\\p{Cntrl}\\u2028\\u2029]", " ")
                .replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sendUdp(String message) throws IOException
    {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        InetAddress address = InetAddress.getByName(host);
        try (DatagramSocket socket = new DatagramSocket())
        {
            socket.send(new DatagramPacket(data, data.length, address, port));
        }
    }

    private void sendTcp(String message) throws IOException
    {
        // 非阻塞连接与写入共用截止时间，避免接收端不读数据时占住发送线程。
        InetSocketAddress address = new InetSocketAddress(host, port);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        try (SocketChannel channel = SocketChannel.open(); Selector selector = Selector.open())
        {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_CONNECT);
            channel.connect(address);
            while (!channel.finishConnect()) await(selector, deadline);
            channel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
            ByteBuffer buffer = ByteBuffer.wrap((message + "\n").getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining())
            {
                if (System.nanoTime() >= deadline) throw new SocketTimeoutException("Syslog send timed out");
                if (channel.write(buffer) == 0) await(selector, deadline);
            }
        }
    }

    private void await(Selector selector, long deadline) throws IOException
    {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) throw new SocketTimeoutException("Syslog send timed out");
        selector.select(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)));
        selector.selectedKeys().clear();
    }

    private void warnFailure(String code)
    {
        long now = System.currentTimeMillis();
        lastFailureAt = now;
        long previous = lastWarning.get();
        if (now - previous >= 10000 && lastWarning.compareAndSet(previous, now))
        {
            // 仅记录错误类别与累计计数，不把查询正文、密钥或完整消息写入运行日志。
            log.warn("Syslog 转发未完成，类别：{}，发送失败：{}，未入队：{}", code, failed.get(), rejected.get());
        }
    }
}
