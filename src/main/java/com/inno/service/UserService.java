package com.inno.service;

import com.inno.service.dto.UserDtoRequest;
import com.inno.service.dto.UserDtoResponse;

import java.util.UUID;

public interface UserService extends BaseService<UserDtoRequest, UserDtoResponse, UUID> {
    UserDtoResponse findByEmail(String email);
}
