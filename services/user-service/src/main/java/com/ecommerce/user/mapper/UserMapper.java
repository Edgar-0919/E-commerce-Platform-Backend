package com.ecommerce.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.user.model.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Delete("DELETE FROM t_user_role WHERE user_id = #{userId}")
    void deleteUserRoles(@Param("userId") Long userId);

    @Insert("INSERT INTO t_user_role (id, user_id, role_id) VALUES (#{id}, #{userId}, #{roleId})")
    void insertUserRole(@Param("id") Long id, @Param("userId") Long userId, @Param("roleId") Long roleId);
}
