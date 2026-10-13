package com.inno.service.impl;

import com.inno.filter.EntitySpecification;
import com.inno.repository.UserRepository;
import com.inno.repository.model.User;
import com.inno.service.UserService;
import com.inno.service.dto.SearchingRequest;
import com.inno.service.dto.UserDtoRequest;
import com.inno.service.dto.UserDtoResponse;
import com.inno.service.exception.NotFoundException;
import com.inno.service.mapper.UserDtoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.inno.service.exception.ExceptionErrorCodes.USER_DOES_NOT_EXIST;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final List<String> fieldsToSearch = List.of("name", "surname");

    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserDtoMapper userDtoMapper) {
        this.userRepository = userRepository;
        this.userDtoMapper = userDtoMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDtoResponse> findAll(SearchingRequest searchingRequest, Pageable pageable) {
        if (searchingRequest == null) {
            LOGGER.info("Reading all users");
            return userRepository.findAll(pageable).map(userDtoMapper::modelToDto);
        }
        LOGGER.info("Reading all users for {}", searchingRequest.getValue());

        Specification<User> specification = EntitySpecification.searchByFields(fieldsToSearch, searchingRequest.getValue());
        return userRepository.findAll(specification, pageable).map(userDtoMapper::modelToDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public UserDtoResponse findById(Long id) {
        LOGGER.info("Reading user by id {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.warn("User with id {} not found", id);
                    return new NotFoundException(String.format(USER_DOES_NOT_EXIST.getErrorMessage(), id));
                });
        return userDtoMapper.modelToDto(user);
    }

    @Override
    @Transactional
    public UserDtoResponse create(UserDtoRequest createRequest) {
        LOGGER.info("Creating user {}", createRequest.toString());

        User model = userDtoMapper.dtoToModel(createRequest);
        User user = userRepository.save(model);
        return userDtoMapper.modelToDto(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserDtoResponse update(Long id, UserDtoRequest updateRequest) {
        LOGGER.info("Updating user with id {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.warn("User with id {} not found. Unable to update user", id);
                    return new NotFoundException(String.format(USER_DOES_NOT_EXIST.getErrorMessage(), id));
                });

        user.setName(updateRequest.getName());
        user.setSurname(updateRequest.getSurname());
        user.setBirthDate(updateRequest.getBirthDate());
        user.setEmail(updateRequest.getEmail());
        user.setActive(updateRequest.isActive());
        return userDtoMapper.modelToDto(userRepository.save(user));
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteById(Long id) {
        LOGGER.info("Deleting user by id {}", id);
        if (!userRepository.existsById(id)) {
            LOGGER.warn("User with id {} not found. Unable to delete user", id);
            throw new NotFoundException(String.format(USER_DOES_NOT_EXIST.getErrorMessage(), id));
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public int updateActiveStatus(Long id, boolean active) {
        LOGGER.info("Updating active status for user with id {}", id);

        if (!userRepository.existsById(id)) {
            LOGGER.warn("User with id {} not found. Unable to update active status", id);
            throw new NotFoundException(String.format(USER_DOES_NOT_EXIST.getErrorMessage(), id));
        }
        return userRepository.updateActiveStatus(id, active);
    }

}
