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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userDtoMapper.modelToDto(user)).thenReturn(response);

        UserDtoResponse result = userService.findById(1L);

        assertSame(response, result);

        verify(userRepository).findById(1L);
        verify(userDtoMapper).modelToDto(user);
    }

    @Test
    void findByIdShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> userService.findById(1L)
        );

        verify(userRepository).findById(1L);
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

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userDtoMapper.modelToDto(user)).thenReturn(response);

        UserDtoResponse result = userService.update(1L, updateRequest);

        assertSame(response, result);
        assertEquals("Petr", user.getName());
        assertEquals("Petrov", user.getSurname());
        assertEquals(
                LocalDate.of(1999, 5, 10),
                user.getBirthDate()
        );
        assertEquals("petr@example.com", user.getEmail());
        assertFalse(user.isActive());

        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
        verify(userDtoMapper).modelToDto(user);
    }

    @Test
    void updateShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> userService.update(1L, request)
        );

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(userDtoMapper);
    }

    // deleteById

    @Test
    void deleteByIdShouldDeleteExistingUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteById(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteByIdShouldThrowWhenUserDoesNotExist() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> userService.deleteById(1L)
        );

        verify(userRepository, never()).deleteById(anyLong());
    }

    // updateActiveStatus

    @Test
    void updateActiveStatusShouldUpdateAndReturnAffectedRows() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.updateActiveStatus(1L, false)).thenReturn(1);

        int result = userService.updateActiveStatus(1L, false);

        assertEquals(1, result);

        verify(userRepository).existsById(1L);
        verify(userRepository).updateActiveStatus(1L, false);
    }

    @Test
    void updateActiveStatusShouldThrowWhenUserDoesNotExist() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> userService.updateActiveStatus(1L, false)
        );

        verify(userRepository, never())
                .updateActiveStatus(anyLong(), anyBoolean());
    }
}