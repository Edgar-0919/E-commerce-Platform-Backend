package com.ecommerce.user.service;

import com.ecommerce.user.model.dto.LoginDTO;
import com.ecommerce.user.model.dto.RegisterDTO;
import com.ecommerce.user.model.vo.LoginVO;
import com.ecommerce.user.model.vo.UserVO;

public interface UserService {

    LoginVO login(LoginDTO dto);

    void register(RegisterDTO dto);

    UserVO getCurrentUserInfo(Long userId);

    void updateUserInfo(Long userId, UserVO dto);
}
