package com.atguigu.service.impl;

import com.atguigu.entity.UserInfo;
import com.atguigu.mapper.UserInfoMapper;
import com.atguigu.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-11-08
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Override
    public UserInfo queryUserFromDb(UserInfo userInfo) {
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        String encodedPWD = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        wrapper.eq("login_name",userInfo.getLoginName()).eq("passwd",encodedPWD);
        UserInfo dbUserInfo = baseMapper.selectOne(wrapper);
        return dbUserInfo;
    }
}
