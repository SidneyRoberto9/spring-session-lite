package io.github.sidneyroberto9.spring_session_lite.service;

import io.github.sidneyroberto9.spring_session_lite.config.SpringSessionLiteProperties;
import lombok.RequiredArgsConstructor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@RequiredArgsConstructor
public class IpHasher {

    private final SpringSessionLiteProperties properties;

    public String hash(String ip) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(properties.getIpHashSalt().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);

            byte[] result = mac.doFinal(ip.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(result);
        } catch (Exception e) {
            throw new IllegalStateException("IP hashing failed", e);
        }
    }
}
