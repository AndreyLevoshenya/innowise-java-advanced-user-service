package com.inno.service.mapper;

import com.inno.repository.model.User;
import com.inno.service.dto.UserDtoRequest;
import com.inno.service.dto.UserDtoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    UserDtoResponse modelToDto(User model);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "paymentCards", ignore = true)
    })
    User dtoToModel(UserDtoRequest dtoRequest);
}
