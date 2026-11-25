package com.inno.service;

import com.inno.service.dto.PaymentCardDtoRequest;
import com.inno.service.dto.PaymentCardDtoResponseWithUser;

import java.util.List;
import java.util.UUID;

public interface PaymentCardService extends BaseService<PaymentCardDtoRequest, PaymentCardDtoResponseWithUser, Long> {
    List<PaymentCardDtoResponseWithUser> findAllByUserId(UUID userId);
}
