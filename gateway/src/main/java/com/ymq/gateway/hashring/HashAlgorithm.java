package com.ymq.gateway.hashring;

/**
 * @author yinmengqi
 * @version 1.0
 * @date 2022/9/26 15:37
 */
public interface HashAlgorithm {

    /**
     * @param key to be hashed
     * @return hash value
     */
    long hash(String key);
}
