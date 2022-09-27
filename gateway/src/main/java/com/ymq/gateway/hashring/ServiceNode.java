package com.ymq.gateway.hashring;

import java.io.Serializable;
import java.util.Objects;

/**
 * 真实节点
 * 可以添加更多特性字段来区分不同的实例，如 DataCenter, Port 等等
 * @author yinmengqi
 * @version 1.0
 * @date 2022/9/26 15:37
 */
public class ServiceNode implements Node, Serializable {

    private static final long serialVersionUID = 5410221835105700427L;

    // 仅用 IP 来作为划分实例的依据
    private final String ip;

    private final Integer port;

    public ServiceNode(String ip,Integer port) {
        this.ip = ip;
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public String getKey() {
        return ip;
    }

    @Override
    public String toString() {
        return getKey();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceNode that = (ServiceNode) o;
        return Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }
}
