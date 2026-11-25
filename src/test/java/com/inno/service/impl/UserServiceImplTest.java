package com.inno.service.impl;

import com.inno.repository.UserRepository;
import com.inno.repository.model.User;
import com.inno.service.dto.SearchingRequest;
import com.inno.service.dto.UserDtoRequest;
import com.inno.service.dto.UserDtoResponse;
import com.inno.service.exception.NotFoundException;
import com.inno.service.mapper.UserDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDtoMapper userDtoMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDtoRequest request;
    private UserDtoResponse response;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Ivan");
        user.setSurname("Ivanov");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setEmail("ivan@example.com");
        user.setActive(true);

        request = new UserDtoRequest();
        request.setName("Ivan");
        request.setSurname("Ivanov");
        request.setBirthDate(user.getBirthDate());
        request.setEmail("ivan@example.com");
        request.setActive(true);

        response = mock(UserDtoResponse.class);
    }

    // findAll

    @Test
    void findAllWithoutSearchShouldReturnAllUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> users = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(users);
        when(userDtoMapper.modelToDto(user)).thenReturn(response);

        Page<UserDtoResponse> result =
                userService.findAll(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));

        verify(userRepository).findAll(pageable);
        verify(userDtoMapper).modelToDto(user);
        verify(userRepository, never())
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findAllWithSearchShouldReturnFilteredUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        SearchingRequest searchingRequest = mock(SearchingRequest.class);

        when(searchingRequest.getValue()).thenReturn("Ivan");

        Page<User> users = new PageImpl<>(List.of(user));

        when(userRepository.findAll(
                any(Specification.class), eq(pageable)
        )).thenReturn(users);

        when(userDtoMapper.modelToDto(user)).thenReturn(response);

        Page<UserDtoResponse> result =
                userService.findAll(searchingRequest, pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));

        verify(userRepository).findAll(
                any(Specification.class), eq(pageable)
        );
        verify(userRepository, never()).findAll(pageable);
    }

    @Test
    void findAllWithSearchShouldReturnEmptyPageWhenNoUsersMatch() {
        Pageable pageable = PageRequest.of(0, 10);
        SearchingRequest searchingRequest = mock(SearchingRequest.class);

        when(searchingRequest.getValue()).thenReturn("Unknown");
        when(userRepository.findAll(
                any(Specification.class), eq(pageable)
        )).thenReturn(Page.empty(pageable));

        Page<UserDtoResponse> result =
                userService.findAll(searchingRequest, pageable);

        assertTrue(result.isEmpty());

        verify(userRepository).findAll(
                any(Specification.class), eq(pageable)
        );
        verifyNoInteractions(userDtoMapper);
    }

    // findById

    @Test
    void findByIdShouldReturnUserWhenFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDtoMapper.modelToDto(user)).thenReturn(response);

        UserDtoResponse result = userService.findById(USER_ID);

        assertSame(response, result);

        verify(userRepository).findById(USER_ID);
        verify(userDtoMapper).modelToDto(user);
    }

    @Test
    void findByIdShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> userService.findById(USER_ID)
        );

        verify(userRepository).findById(USER_ID);
        verifyNoInteractions(userDtoMapper);
    }

    // create

    @Test
    void createShouldMapSaveAndReturnUser() {
        when(userDtoMapper.dtoToModel(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userDtoMapper.modelToDto(user)).thenReturn(response);

        UserDtoResponse result = userService.create(request);

        assertSame(response, result);

        verify(userDtoMapper).dtoToModel(request);
        verify(userRepository).save(user);
        verify(userDtoMapper).modelToDto(user);
    }

    // update

    @Test
    void updateShouldModifyExistingUser() {
        UserDtoRequest updateRequest = new UserDtoRequest();
        updateRequest.setName("Petr");
        updateRequest.setSurname("Petrov");
        updateRequest.setBirthDate(LocalDate.of(1999, 5, 10));
        updateRequest.setEmail("petr@example.com");
        updateRequest.setActive(false);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userDtoMapper.modelToDto(user)).thenReturn(response);

        UserDtoResponse result = userService.update(USER_ID, updateRequest);

        assertSame(response, result);
        assertEquals("Petr", user.getName());
        assertEquals("Petrov", user.getSurname());
        assertEquals(
                LocalDate.of(1999, 5, 10),
                user.getBirthDate()
        );
        assertEquals("petr@example.com", user.getEmail());
        assertFalse(user.isActive());

        verify(userRepository).findById(USER_ID);
        verify(userRepository).save(user);
        verify(userDtoMapper).modelToDto(user);
    }

    @Test
    void updateShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> userService.update(USER_ID, request)
        );

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(userDtoMapper);
    }

    // deleteById

    @Test
    void deleteByIdShouldDeleteExistingUser() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        userService.deleteById(USER_ID);

        verify(userRepository).existsById(USER_ID);
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    void deleteByIdShouldThrowWhenUserDoesNotExist() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> userService.deleteById(USER_ID)
        );

        verify(userRepository, never()).deleteById(any());
    }

    // updateActiveStatus

    @Test
    void updateActiveStatusShouldUpdateAndReturnAffectedRows() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userRepository.updateActiveStatus(USER_ID, false)).thenReturn(1);

        int result = userService.updateActiveStatus(USER_ID, false);

        assertEquals(1, result);

        verify(userRepository).existsById(USER_ID);
        verify(userRepository).updateActiveStatus(USER_ID, false);
    }

    @Test
    void updateActiveStatusShouldThrowWhenUserDoesNotExist() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> userService.updateActiveStatus(USER_ID, false)
        );

        verify(userRepository, never())
                .updateActiveStatus(any(), anyBoolean());
    }

    // findByEmail

    @Test
    void findByEmailShouldReturnUserWhenFound() {
        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));
        when(userDtoMapper.modelToDto(user)).thenReturn(response);

        UserDtoResponse result = userService.findByEmail("ivan@example.com");

        assertSame(response, result);

        verify(userRepository).findByEmail("ivan@example.com");
        verify(userDtoMapper).modelToDto(user);
    }

    @Test
    void findByEmailShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail("invalid@example.com")).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> userService.findByEmail("invalid@example.com")
        );

        verify(userRepository).findByEmail("invalid@example.com");
        verifyNoInteractions(userDtoMapper);
    }
}