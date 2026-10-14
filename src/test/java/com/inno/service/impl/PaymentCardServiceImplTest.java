package com.inno.service.impl;

import com.inno.repository.PaymentCardRepository;
import com.inno.repository.UserRepository;
import com.inno.repository.model.PaymentCard;
import com.inno.repository.model.User;
import com.inno.service.dto.PaymentCardDtoRequest;
import com.inno.service.dto.PaymentCardDtoResponseWithUser;
import com.inno.service.dto.SearchingRequest;
import com.inno.service.exception.NotFoundException;
import com.inno.service.exception.TooManyPaymentCardsException;
import com.inno.service.mapper.PaymentCardDtoMapper;
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

import static org.junit.jupiter.api.Assertions.*;
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
class PaymentCardServiceImplTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardDtoMapper paymentCardDtoMapper;

    @InjectMocks
    private PaymentCardServiceImpl paymentCardService;

    private User user;
    private PaymentCard paymentCard;
    private PaymentCardDtoRequest request;
    private PaymentCardDtoResponseWithUser response;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Ivan");
        user.setSurname("Ivanov");
        user.setEmail("ivan@example.com");
        user.setActive(true);

        paymentCard = new PaymentCard();
        paymentCard.setUser(user);
        paymentCard.setNumber("1234567890123456");
        paymentCard.setHolder("IVAN IVANOV");
        paymentCard.setExpirationDate(LocalDate.of(2030, 10, 1));
        paymentCard.setActive(true);

        // Assumes the entity initializes paymentCards to an empty list.
        user.getPaymentCards().add(paymentCard);

        request = new PaymentCardDtoRequest();
        request.setUserId(1L);
        request.setNumber("1234567890123456");
        request.setHolder("IVAN IVANOV");
        request.setExpirationDate(LocalDate.of(2030, 10, 1));
        request.setActive(true);

        response = mock(PaymentCardDtoResponseWithUser.class);
    }

    // findAll

    @Test
    void findAllWithoutSearchShouldReturnAllCards() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCard> cards = new PageImpl<>(List.of(paymentCard));

        when(paymentCardRepository.findAll(pageable)).thenReturn(cards);
        when(paymentCardDtoMapper.modelToDtoWithUser(paymentCard))
                .thenReturn(response);

        Page<PaymentCardDtoResponseWithUser> result =
                paymentCardService.findAll(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));

        verify(paymentCardRepository).findAll(pageable);
        verify(paymentCardDtoMapper).modelToDtoWithUser(paymentCard);
        verify(paymentCardRepository, never())
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findAllWithSearchShouldReturnFilteredCards() {
        Pageable pageable = PageRequest.of(0, 10);
        SearchingRequest searchingRequest = mock(SearchingRequest.class);

        when(searchingRequest.getValue()).thenReturn("IVAN");

        Page<PaymentCard> cards = new PageImpl<>(List.of(paymentCard));

        when(paymentCardRepository.findAll(
                any(Specification.class), eq(pageable)
        )).thenReturn(cards);

        when(paymentCardDtoMapper.modelToDtoWithUser(paymentCard))
                .thenReturn(response);

        Page<PaymentCardDtoResponseWithUser> result =
                paymentCardService.findAll(searchingRequest, pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));

        verify(paymentCardRepository).findAll(
                any(Specification.class), eq(pageable)
        );
        verify(paymentCardRepository, never()).findAll(pageable);
    }

    @Test
    void findAllWithSearchShouldReturnEmptyPageWhenNoCardsMatch() {
        Pageable pageable = PageRequest.of(0, 10);
        SearchingRequest searchingRequest = mock(SearchingRequest.class);

        when(searchingRequest.getValue()).thenReturn("UNKNOWN");

        when(paymentCardRepository.findAll(
                any(Specification.class), eq(pageable)
        )).thenReturn(Page.empty(pageable));

        Page<PaymentCardDtoResponseWithUser> result =
                paymentCardService.findAll(searchingRequest, pageable);

        assertTrue(result.isEmpty());
        verifyNoInteractions(paymentCardDtoMapper);
    }

    // findById

    @Test
    void findByIdShouldReturnCardWhenFound() {
        when(paymentCardRepository.findById(10L))
                .thenReturn(Optional.of(paymentCard));

        when(paymentCardDtoMapper.modelToDtoWithUser(paymentCard))
                .thenReturn(response);

        PaymentCardDtoResponseWithUser result =
                paymentCardService.findById(10L);

        assertSame(response, result);

        verify(paymentCardRepository).findById(10L);
        verify(paymentCardDtoMapper).modelToDtoWithUser(paymentCard);
    }

    @Test
    void findByIdShouldThrowWhenCardDoesNotExist() {
        when(paymentCardRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> paymentCardService.findById(10L)
        );

        verifyNoInteractions(paymentCardDtoMapper);
    }

    // create

    @Test
    void createShouldSaveCardAndReturnResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(paymentCardDtoMapper.dtoToModel(request, userRepository))
                .thenReturn(paymentCard);

        when(paymentCardRepository.save(paymentCard))
                .thenReturn(paymentCard);

        when(paymentCardDtoMapper.modelToDtoWithUser(paymentCard))
                .thenReturn(response);

        PaymentCardDtoResponseWithUser result =
                paymentCardService.create(request);

        assertSame(response, result);

        verify(userRepository).findById(1L);
        verify(paymentCardDtoMapper).dtoToModel(request, userRepository);
        verify(paymentCardRepository).save(paymentCard);
        verify(paymentCardDtoMapper).modelToDtoWithUser(paymentCard);
    }

    @Test
    void createShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> paymentCardService.create(request)
        );

        verify(paymentCardRepository, never())
                .save(any(PaymentCard.class));
        verifyNoInteractions(paymentCardDtoMapper);
    }

    @Test
    void createShouldAllowFifthCard() {
        // Arrange
        user.getPaymentCards().clear();
        for (int i = 0; i < 4; i++) {
            user.getPaymentCards().add(new PaymentCard());
        }

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardDtoMapper.dtoToModel(request, userRepository)).thenReturn(paymentCard);
        when(paymentCardRepository.save(paymentCard)).thenReturn(paymentCard);
        when(paymentCardDtoMapper.modelToDtoWithUser(paymentCard)).thenReturn(response);

        // Act
        PaymentCardDtoResponseWithUser result = paymentCardService.create(request);

        // Assert
        assertNotNull(result);
        assertEquals(response, result);

        verify(paymentCardRepository).save(paymentCard);
        verify(paymentCardDtoMapper).dtoToModel(request, userRepository);
        verify(paymentCardDtoMapper).modelToDtoWithUser(paymentCard);
    }

    @Test
    void createShouldThrowWhenUserAlreadyHasFiveCards() {
        user.getPaymentCards().clear();

        for (int i = 0; i < 5; i++) {
            PaymentCard card = new PaymentCard();
            card.setUser(user);
            user.getPaymentCards().add(card);
        }

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(
                TooManyPaymentCardsException.class,
                () -> paymentCardService.create(request)
        );

        verify(paymentCardRepository, never())
                .save(any(PaymentCard.class));
        verifyNoInteractions(paymentCardDtoMapper);
    }

    // update

    @Test
    void updateShouldModifyExistingCard() {
        request.setNumber("9999888877776666");
        request.setHolder("PETR PETROV");
        request.setActive(false);

        when(paymentCardRepository.findById(10L))
                .thenReturn(Optional.of(paymentCard));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(paymentCardRepository.save(paymentCard))
                .thenReturn(paymentCard);

        when(paymentCardDtoMapper.modelToDtoWithUser(paymentCard))
                .thenReturn(response);

        PaymentCardDtoResponseWithUser result =
                paymentCardService.update(10L, request);

        assertSame(response, result);
        assertSame(user, paymentCard.getUser());
        assertEquals("9999888877776666", paymentCard.getNumber());
        assertEquals("PETR PETROV", paymentCard.getHolder());
        assertEquals(LocalDate.of(2030, 10, 1), paymentCard.getExpirationDate());
        assertFalse(paymentCard.isActive());

        verify(paymentCardRepository).findById(10L);
        verify(userRepository).findById(1L);
        verify(paymentCardRepository).save(paymentCard);
        verify(paymentCardDtoMapper).modelToDtoWithUser(paymentCard);
    }

    @Test
    void updateShouldThrowWhenCardDoesNotExist() {
        when(paymentCardRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> paymentCardService.update(10L, request)
        );

        verifyNoInteractions(userRepository, paymentCardDtoMapper);
        verify(paymentCardRepository, never())
                .save(any(PaymentCard.class));
    }

    @Test
    void updateShouldThrowWhenTargetUserDoesNotExist() {
        when(paymentCardRepository.findById(10L))
                .thenReturn(Optional.of(paymentCard));

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> paymentCardService.update(10L, request)
        );

        verify(paymentCardRepository, never())
                .save(any(PaymentCard.class));
        verifyNoInteractions(paymentCardDtoMapper);
    }

    // deleteById

    @Test
    void deleteByIdShouldDeleteExistingCard() {
        when(paymentCardRepository.existsById(10L)).thenReturn(true);

        paymentCardService.deleteById(10L);

        verify(paymentCardRepository).existsById(10L);
        verify(paymentCardRepository).deleteById(10L);
    }

    @Test
    void deleteByIdShouldThrowWhenCardDoesNotExist() {
        when(paymentCardRepository.existsById(10L)).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> paymentCardService.deleteById(10L)
        );

        verify(paymentCardRepository, never()).deleteById(anyLong());
    }

    // updateActiveStatus

    @Test
    void updateActiveStatusShouldUpdateAndReturnAffectedRows() {
        when(paymentCardRepository.existsById(10L)).thenReturn(true);

        when(paymentCardRepository.updateActiveStatus(10L, false))
                .thenReturn(1);

        int result = paymentCardService.updateActiveStatus(10L, false);

        assertEquals(1, result);

        verify(paymentCardRepository).existsById(10L);
        verify(paymentCardRepository).updateActiveStatus(10L, false);
    }

    @Test
    void updateActiveStatusShouldThrowWhenCardDoesNotExist() {
        when(paymentCardRepository.existsById(10L)).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> paymentCardService.updateActiveStatus(10L, false)
        );

        verify(paymentCardRepository, never())
                .updateActiveStatus(anyLong(), anyBoolean());
    }

    // findAllByUserId

    @Test
    void findAllByUserIdShouldReturnCardsForExistingUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        when(paymentCardRepository.findAllByUserId(1L))
                .thenReturn(List.of(paymentCard));

        when(paymentCardDtoMapper.modelToDtoWithUser(paymentCard))
                .thenReturn(response);

        List<PaymentCardDtoResponseWithUser> result =
                paymentCardService.findAllByUserId(1L);

        assertEquals(1, result.size());
        assertSame(response, result.get(0));

        verify(userRepository).existsById(1L);
        verify(paymentCardRepository).findAllByUserId(1L);
        verify(paymentCardDtoMapper).modelToDtoWithUser(paymentCard);
    }

    @Test
    void findAllByUserIdShouldReturnEmptyListWhenUserHasNoCards() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(paymentCardRepository.findAllByUserId(1L))
                .thenReturn(List.of());

        List<PaymentCardDtoResponseWithUser> result =
                paymentCardService.findAllByUserId(1L);

        assertTrue(result.isEmpty());

        verify(paymentCardRepository).findAllByUserId(1L);
        verifyNoInteractions(paymentCardDtoMapper);
    }

    @Test
    void findAllByUserIdShouldThrowWhenUserDoesNotExist() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> paymentCardService.findAllByUserId(1L)
        );

        verify(paymentCardRepository, never()).findAllByUserId(anyLong());
        verifyNoInteractions(paymentCardDtoMapper);
    }
}