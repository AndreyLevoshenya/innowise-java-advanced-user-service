package com.inno.service;

import com.inno.service.dto.PaymentCardDtoRequest;
import com.inno.service.dto.PaymentCardDtoResponseWithUser;

import java.util.List;

public interface PaymentCardService extends BaseService<PaymentCardDtoRequest, PaymentCardDtoResponseWithUser, Long> {
    List<PaymentCardDtoResponseWithUser> findAllByUserId(Long userId);
}
