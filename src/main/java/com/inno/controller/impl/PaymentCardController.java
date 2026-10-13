package com.inno.controller.impl;

import com.inno.controller.BaseController;
import com.inno.service.PaymentCardService;
import com.inno.service.dto.PaymentCardDtoRequest;
import com.inno.service.dto.PaymentCardDtoResponse;
import com.inno.service.dto.SearchingRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.inno.controller.RestConstants.PAYMENT_CARDS_V1_API_PATH;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping(value = PAYMENT_CARDS_V1_API_PATH)
public class PaymentCardController implements BaseController<PaymentCardDtoRequest, PaymentCardDtoResponse, Long> {
    private final PaymentCardService paymentCardService;

    @Autowired
    public PaymentCardController(PaymentCardService paymentCardService) {
        this.paymentCardService = paymentCardService;
    }


    @Override
    @GetMapping
    @ResponseStatus(OK)
    public ResponseEntity<Page<PaymentCardDtoResponse>> findAll(
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(sort = "number", direction = Sort.Direction.ASC) Pageable pageable) {
        SearchingRequest searchingRequest = null;
        if (search != null && !search.isBlank()) {
            searchingRequest = new SearchingRequest(search);
        }
        Page<PaymentCardDtoResponse> pageDtoResponse = paymentCardService.findAll(searchingRequest, pageable);
        return new ResponseEntity<>(pageDtoResponse, OK);
    }

    @Override
    @GetMapping(value = "/{id}")
    @ResponseStatus(OK)
    public ResponseEntity<PaymentCardDtoResponse> findById(@PathVariable Long id) {
        PaymentCardDtoResponse paymentCardDtoResponse = paymentCardService.findById(id);
        return new ResponseEntity<>(paymentCardDtoResponse, OK);
    }

    @Override
    @PostMapping
    @ResponseStatus(CREATED)
    public ResponseEntity<PaymentCardDtoResponse> create(@Valid @RequestBody PaymentCardDtoRequest createRequest) {
        PaymentCardDtoResponse paymentCardDtoResponse = paymentCardService.create(createRequest);
        return new ResponseEntity<>(paymentCardDtoResponse, CREATED);
    }

    @Override
    @PutMapping(value = "/{id}")
    @ResponseStatus(OK)
    public ResponseEntity<PaymentCardDtoResponse> update(@PathVariable Long id, @Valid @RequestBody PaymentCardDtoRequest updateRequest) {
        PaymentCardDtoResponse paymentCardDtoResponse = paymentCardService.update(id, updateRequest);
        return new ResponseEntity<>(paymentCardDtoResponse, OK);
    }

    @Override
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(NO_CONTENT)
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        paymentCardService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        paymentCardService.updateActiveStatus(id, active);
        return ResponseEntity.noContent().build();
    }
}
