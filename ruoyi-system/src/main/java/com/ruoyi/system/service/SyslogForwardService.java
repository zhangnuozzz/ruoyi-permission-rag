package com.ruoyi.system.service;

import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ruoyi.system.domain.SysRagBehaviorAlert;

/**
 * VACP Syslog 网络转发服务。
 *
 * 支持 UDP / TCP。
 * 转发失败不能影响 RAG 检索、审计和告警主流程。
 */
@Service
public class SyslogForwardService
{
    @Value("${vacp.syslog.enabled:false}")
    private boolean enabled;

    @Value("${vacp.syslog.host:127.0.0.1}")
    private String host;

    @Value("${vacp.syslog.port:5514}")
    private int port;

    @Value("${vacp.syslog.protocol:UDP}")
    private String protocol;

    /**
     * 转发一条行为告警。
     */
    public boolean forwardAlert(SysRagBehaviorAlert alert)
    {
        if (!enabled || alert == null)
        {
            return false;
        }

        try
        {
            String message = buildMessage(alert);

            if ("TCP".equalsIgnoreCase(protocol))
            {
                sendTcp(message);
            }
            else
            {
                sendUdp(message);
            }

            return true;
        }
        catch (Exception e)
        {
            /*
             * Syslog 属于外部审计通道。
             * 外部日志服务器异常不能阻塞平台主业务。
             */
            return false;
        }
    }

    /**
     * 构造 RFC3164 风格 Syslog 消息。
     */
    private String buildMessage(SysRagBehaviorAlert alert)
    {
        String timestamp = new SimpleDateFormat(
                "MMM dd HH:mm:ss",
                Locale.ENGLISH
        ).format(new Date());

        String hostname = getHostname();

        StringBuilder builder = new StringBuilder();

        /*
         * PRI=134：
         * facility = local0
         * severity = informational
         */
        builder.append("<134>");
        builder.append(timestamp).append(" ");
        builder.append(hostname).append(" ");
        builder.append("VACP-RAG-AUDIT: ");

        builder.append("alert_id=").append(alert.getId());
        builder.append(" source_log_id=").append(alert.getSourceLogId());
        builder.append(" user_id=").append(alert.getUserId());

        builder.append(" user_name=\"")
                .append(escape(alert.getUserName()))
                .append("\"");

        builder.append(" alert_type=")
                .append(escape(alert.getAlertType()));

        builder.append(" alert_level=")
                .append(escape(alert.getAlertLevel()));

        builder.append(" status=")
                .append(escape(alert.getStatus()));

        builder.append(" allow_access=")
                .append(escape(alert.getAllowAccess()));

        builder.append(" cost_time=")
                .append(alert.getCostTime());

        builder.append(" query=\"")
                .append(escape(alert.getQueryText()))
                .append("\"");

        builder.append(" reason=\"")
                .append(escape(alert.getAlertReason()))
                .append("\"");

        return builder.toString();
    }

    /**
     * UDP 转发。
     */
    private void sendUdp(String message) throws Exception
    {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        InetAddress address = InetAddress.getByName(host);

        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                address,
                port
        );

        try (DatagramSocket socket = new DatagramSocket())
        {
            socket.send(packet);
        }
    }

    /**
     * TCP 转发。
     */
    private void sendTcp(String message) throws Exception
    {
        try (Socket socket = new Socket())
        {
            socket.connect(
                    new InetSocketAddress(host, port),
                    1000
            );

            OutputStream output = socket.getOutputStream();

            output.write(
                    (message + "\n").getBytes(StandardCharsets.UTF_8)
            );

            output.flush();
        }
    }

    private String getHostname()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (Exception e)
        {
            return "vacp-platform";
        }
    }

    private String escape(String value)
    {
        if (value == null)
        {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
