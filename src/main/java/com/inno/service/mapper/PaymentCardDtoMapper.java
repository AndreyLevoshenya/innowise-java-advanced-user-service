package com.inno.service.mapper;

import com.inno.repository.UserRepository;
import com.inno.repository.model.PaymentCard;
import com.inno.service.dto.PaymentCardDtoRequest;
import com.inno.service.dto.PaymentCardDtoResponse;
import com.inno.service.dto.PaymentCardDtoResponseWithUser;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface PaymentCardDtoMapper {
    PaymentCardDtoResponse modelToDto(PaymentCard model);

    PaymentCardDtoResponseWithUser modelToDtoWithUser(PaymentCard model);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "user", expression = "java(userRepository.getReferenceById(dtoRequest.getUserId()))"),})
    PaymentCard dtoToModel(
            PaymentCardDtoRequest dtoRequest,
            @Context UserRepository userRepository);
}
