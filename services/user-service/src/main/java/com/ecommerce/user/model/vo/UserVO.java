package com.ecommerce.user.model.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    private List<String> roles;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private Integer merchantApplicationStatus;
    private String merchantApplicationStatusText;
}
