package com.ecommerce.core.model;

import lombok.Data;
import java.util.List;

@Data
public class TokenPayload {

    private Long userId;
    private String username;
    private String nickname;
    private List<String> roles;
    private Long merchantId;
    private Long iat;
    private Long exp;
}
