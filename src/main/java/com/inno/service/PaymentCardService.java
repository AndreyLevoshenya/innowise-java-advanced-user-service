package com.inno.service;

import com.inno.service.dto.PaymentCardDtoRequest;
import com.inno.service.dto.PaymentCardDtoResponse;

import java.util.List;

public interface PaymentCardService extends BaseService<PaymentCardDtoRequest, PaymentCardDtoResponse, Long> {
    List<PaymentCardDtoResponse> findAllByUserId(Long userId);
}
