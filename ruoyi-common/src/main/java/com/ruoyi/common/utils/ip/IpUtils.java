package com.ruoyi.common.utils.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;
import com.ruoyi.common.utils.StringUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 获取IP方法
 * 
 * @author ruoyi
 */
@Component
public class IpUtils
{
    private static volatile Set<String> trustedProxies = Collections.emptySet();

    /** 只信任明确登记的直连代理，默认不接收客户端转发头。 */
    @Value("${security.trusted-proxies:}")
    public void setTrustedProxies(String addresses)
    {
        Set<String> configured = new HashSet<>();
        if (addresses != null && !addresses.trim().isEmpty())
        {
            for (String address : addresses.split(",", -1))
            {
                String normalized = normalizeAddress(address);
                if (normalized == null)
                {
                    throw new IllegalArgumentException("security.trusted-proxies 必须为逗号分隔的 IP 地址");
                }
                configured.add(normalized);
            }
        }
        trustedProxies = Collections.unmodifiableSet(configured);
    }

    public static String getIpAddr(HttpServletRequest request)
    {
        if (request == null)
        {
            return "unknown";
        }
        String peer = normalizeAddress(request.getRemoteAddr());
        if (peer == null)
        {
            return "unknown";
        }
        Set<String> proxies = trustedProxies;
        if (!proxies.contains(peer))
        {
            return peer;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.trim().isEmpty())
        {
            return peer;
        }
        if (forwarded.length() > 1024)
        {
            return "unknown";
        }
        String[] hops = forwarded.split(",", -1);
        if (hops.length > 20)
        {
            return "unknown";
        }
        String[] normalized = new String[hops.length];
        for (int i = 0; i < hops.length; i++)
        {
            normalized[i] = normalizeAddress(hops[i]);
            if (normalized[i] == null)
            {
                return "unknown";
            }
        }
        for (int i = normalized.length - 1; i >= 0; i--)
        {
            if (!proxies.contains(normalized[i]) || i == 0)
            {
                return normalized[i];
            }
        }
        return peer;
    }

    /** 仅解析单个 IPv4/IPv6 字面地址，不解析主机名、网段或带端口的地址。 */
    public static String normalizeAddress(String value)
    {
        if (value == null)
        {
            return null;
        }
        String address = value.trim();
        if (address.isEmpty() || address.length() > 45)
        {
            return null;
        }
        if (address.indexOf(':') >= 0)
        {
            if (!address.matches("[0-9a-fA-F:.]+"))
            {
                return null;
            }
            try
            {
                InetAddress parsed = InetAddress.getByName(address);
                return parsed.isLoopbackAddress() && address.indexOf('.') < 0
                        ? "127.0.0.1" : parsed.getHostAddress();
            }
            catch (UnknownHostException e)
            {
                return null;
            }
        }
        String[] parts = address.split("\\.", -1);
        if (parts.length != 4)
        {
            return null;
        }
        for (String part : parts)
        {
            if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255)
            {
                return null;
            }
        }
        return address;
    }

    public static boolean internalIp(String ip)
    {
        byte[] addr = textToNumericFormatV4(ip);
        return internalIp(addr) || "127.0.0.1".equals(ip);
    }

    private static boolean internalIp(byte[] addr)
    {
        if (StringUtils.isNull(addr) || addr.length < 2)
        {
            return true;
        }
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        // 10.x.x.x/8
        final byte SECTION_1 = 0x0A;
        // 172.16.x.x/12
        final byte SECTION_2 = (byte) 0xAC;
        final byte SECTION_3 = (byte) 0x10;
        final byte SECTION_4 = (byte) 0x1F;
        // 192.168.x.x/16
        final byte SECTION_5 = (byte) 0xC0;
        final byte SECTION_6 = (byte) 0xA8;
        switch (b0)
        {
            case SECTION_1:
                return true;
            case SECTION_2:
                if (b1 >= SECTION_3 && b1 <= SECTION_4)
                {
                    return true;
                }
            case SECTION_5:
                switch (b1)
                {
                    case SECTION_6:
                        return true;
                }
            default:
                return false;
        }
    }

    /**
     * 将IPv4地址转换成字节
     * 
     * @param text IPv4地址
     * @return byte 字节
     */
    public static byte[] textToNumericFormatV4(String text)
    {
        if (text.length() == 0)
        {
            return null;
        }

        byte[] bytes = new byte[4];
        String[] elements = text.split("\\.", -1);
        try
        {
            long l;
            int i;
            switch (elements.length)
            {
                case 1:
                    l = Long.parseLong(elements[0]);
                    if ((l < 0L) || (l > 4294967295L)) {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l >> 24 & 0xFF);
                    bytes[1] = (byte) (int) ((l & 0xFFFFFF) >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 2:
                    l = Integer.parseInt(elements[0]);
                    if ((l < 0L) || (l > 255L)) {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l & 0xFF);
                    l = Integer.parseInt(elements[1]);
                    if ((l < 0L) || (l > 16777215L)) {
                        return null;
                    }
                    bytes[1] = (byte) (int) (l >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 3:
                    for (i = 0; i < 2; ++i)
                    {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L)) {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    l = Integer.parseInt(elements[2]);
                    if ((l < 0L) || (l > 65535L)) {
                        return null;
                    }
                    bytes[2] = (byte) (int) (l >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 4:
                    for (i = 0; i < 4; ++i)
                    {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L)) {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    break;
                default:
                    return null;
            }
        }
        catch (NumberFormatException e)
        {
            return null;
        }
        return bytes;
    }

    public static String getHostIp()
    {
        try
        {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
        }
        return "127.0.0.1";
    }

    public static String getHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
        }
        return "未知";
    }
}