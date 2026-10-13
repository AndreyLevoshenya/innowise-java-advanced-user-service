package com.inno.service.impl;

import com.inno.filter.EntitySpecification;
import com.inno.repository.PaymentCardRepository;
import com.inno.repository.UserRepository;
import com.inno.repository.model.PaymentCard;
import com.inno.repository.model.User;
import com.inno.service.PaymentCardService;
import com.inno.service.dto.PaymentCardDtoRequest;
import com.inno.service.dto.PaymentCardDtoResponseWithUser;
import com.inno.service.dto.SearchingRequest;
import com.inno.service.exception.NotFoundException;
import com.inno.service.exception.TooManyPaymentCardsException;
import com.inno.service.mapper.PaymentCardDtoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.inno.service.exception.ExceptionErrorCodes.PAYMENT_CARD_DOES_NOT_EXIST;
import static com.inno.service.exception.ExceptionErrorCodes.TOO_MANY_PAYMENT_CARDS_EXCEPTION;
import static com.inno.service.exception.ExceptionErrorCodes.USER_DOES_NOT_EXIST;

@Service
public class PaymentCardServiceImpl implements PaymentCardService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentCardServiceImpl.class);
    private static final List<String> fieldsToSearch = List.of("number", "holder");

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardDtoMapper paymentCardDtoMapper;

    @Autowired
    public PaymentCardServiceImpl(PaymentCardRepository paymentCardRepository, UserRepository userRepository, PaymentCardDtoMapper paymentCardDtoMapper) {
        this.paymentCardRepository = paymentCardRepository;
        this.userRepository = userRepository;
        this.paymentCardDtoMapper = paymentCardDtoMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentCardDtoResponseWithUser> findAll(SearchingRequest searchingRequest, Pageable pageable) {
        if (searchingRequest == null) {
            LOGGER.info("Reading all payment cards");
            return paymentCardRepository.findAll(pageable).map(paymentCardDtoMapper::modelToDtoWithUser);
        }
        LOGGER.info("Reading all payment cards for {}", searchingRequest.getValue());

        Specification<PaymentCard> specification = EntitySpecification.searchByFields(fieldsToSearch, searchingRequest.getValue());
        return paymentCardRepository.findAll(specification, pageable).map(paymentCardDtoMapper::modelToDtoWithUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentCardDtoResponseWithUser findById(Long id) {
        LOGGER.info("Reading payment card by id {}", id);
        PaymentCard paymentCard = paymentCardRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.warn("Payment card with id {} not found", id);
                    return new NotFoundException(String.format(PAYMENT_CARD_DOES_NOT_EXIST.getErrorMessage(), id));
                });
        return paymentCardDtoMapper.modelToDtoWithUser(paymentCard);
    }

    @Override
    @Transactional
    public PaymentCardDtoResponseWithUser create(PaymentCardDtoRequest createRequest) {
        LOGGER.info("Creating payment card {}", createRequest.toString());

        User user = userRepository.findById(createRequest.getUserId()).orElseThrow(() -> {
            LOGGER.error("User with id {} not found. Unable to create payment card", createRequest.getUserId());
            return new NotFoundException(String.format(USER_DOES_NOT_EXIST.getErrorMessage(), createRequest.getUserId()));
        });

        if (user.getPaymentCards().size() >= 5) {
            LOGGER.error("User with id {} has 5 cards. Unable to create payment card", createRequest.getUserId());
            throw new TooManyPaymentCardsException(String.format(TOO_MANY_PAYMENT_CARDS_EXCEPTION.getErrorMessage(), createRequest.getUserId()));
        }

        PaymentCard model = paymentCardDtoMapper.dtoToModel(createRequest, userRepository);
        PaymentCard paymentCard = paymentCardRepository.save(model);
        return paymentCardDtoMapper.modelToDtoWithUser(paymentCard);
    }

    @Override
    @Transactional
    public PaymentCardDtoResponseWithUser update(Long id, PaymentCardDtoRequest updateRequest) {
        LOGGER.info("Updating payment card with id {}", id);

        PaymentCard paymentCard = paymentCardRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.warn("Payment card with id {} not found. Unable to update payment card", id);
                    return new NotFoundException(String.format(PAYMENT_CARD_DOES_NOT_EXIST.getErrorMessage(), id));
                });

        User user = userRepository.findById(updateRequest.getUserId()).orElseThrow(() -> {
            LOGGER.error("User with id {} not found. Unable to update payment card", updateRequest.getUserId());
            return new NotFoundException(String.format(USER_DOES_NOT_EXIST.getErrorMessage(), updateRequest.getUserId()));
        });

        paymentCard.setUser(user);
        paymentCard.setNumber(updateRequest.getNumber());
        paymentCard.setHolder(updateRequest.getHolder());
        paymentCard.setExpirationDate(updateRequest.getExpirationDate());
        paymentCard.setActive(updateRequest.isActive());
        return paymentCardDtoMapper.modelToDtoWithUser(paymentCardRepository.save(paymentCard));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        LOGGER.info("Deleting payment card by id {}", id);
        if (!paymentCardRepository.existsById(id)) {
            LOGGER.warn("Payment card with id {} not found. Unable to delete payment card", id);
            throw new NotFoundException(String.format(PAYMENT_CARD_DOES_NOT_EXIST.getErrorMessage(), id));
        }
        paymentCardRepository.deleteById(id);
    }

    @Override
    @Transactional
    public int updateActiveStatus(Long id, boolean active) {
        LOGGER.info("Updating active status for payment card with id {}", id);

        if (!paymentCardRepository.existsById(id)) {
            LOGGER.warn("Payment card with id {} not found. Unable to update active status", id);
            throw new NotFoundException(String.format(PAYMENT_CARD_DOES_NOT_EXIST.getErrorMessage(), id));
        }
        return paymentCardRepository.updateActiveStatus(id, active);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentCardDtoResponseWithUser> findAllByUserId(Long userId) {
        LOGGER.info("Reading payment cards by user id {}", userId);
        if (!userRepository.existsById(userId)) {
            LOGGER.error("User with id {} not found. Unable to read payment cards", userId);
            throw new NotFoundException(String.format(USER_DOES_NOT_EXIST.getErrorMessage(), userId));
        }
        return paymentCardRepository.findAllByUserId(userId).stream().map(paymentCardDtoMapper::modelToDtoWithUser).toList();
    }
}
