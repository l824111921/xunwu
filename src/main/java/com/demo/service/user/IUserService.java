package com.demo.service.user;

import com.demo.entity.User;
import com.demo.service.ServiceResult;
import com.demo.web.dto.UserDTO;

public interface IUserService {

    User findUserByName(String userName);

    ServiceResult<UserDTO> findById(Long adminId);
}
