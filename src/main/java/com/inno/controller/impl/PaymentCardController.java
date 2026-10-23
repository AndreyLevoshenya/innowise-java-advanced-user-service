package com.inno.controller.impl;

import com.inno.controller.BaseController;
import com.inno.service.PaymentCardService;
import com.inno.service.dto.PaymentCardDtoRequest;
import com.inno.service.dto.PaymentCardDtoResponseWithUser;
import com.inno.service.dto.SearchingRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.inno.controller.RestConstants.PAYMENT_CARDS_V1_API_PATH;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping(value = PAYMENT_CARDS_V1_API_PATH)
public class PaymentCardController implements BaseController<PaymentCardDtoRequest, PaymentCardDtoResponseWithUser, Long> {
    private final PaymentCardService paymentCardService;

    @Autowired
    public PaymentCardController(PaymentCardService paymentCardService) {
        this.paymentCardService = paymentCardService;
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<PaymentCardDtoResponseWithUser>> findAll(
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(sort = "number", direction = Sort.Direction.ASC) Pageable pageable) {
        SearchingRequest searchingRequest = null;
        if (search != null && !search.isBlank()) {
            searchingRequest = new SearchingRequest(search);
        }
        Page<PaymentCardDtoResponseWithUser> pageDtoResponse = paymentCardService.findAll(searchingRequest, pageable);
        return ResponseEntity.ok(pageDtoResponse);
    }

    @Override
    @GetMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PaymentCardDtoResponseWithUser> findById(@PathVariable Long id) {
        PaymentCardDtoResponseWithUser paymentCardDtoResponse = paymentCardService.findById(id);
        return ResponseEntity.ok(paymentCardDtoResponse);
    }

    @Override
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PaymentCardDtoResponseWithUser> create(@Valid @RequestBody PaymentCardDtoRequest createRequest) {
        PaymentCardDtoResponseWithUser paymentCardDtoResponse = paymentCardService.create(createRequest);
        return ResponseEntity.status(CREATED).body(paymentCardDtoResponse);
    }

    @Override
    @PutMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PaymentCardDtoResponseWithUser> update(@PathVariable Long id, @Valid @RequestBody PaymentCardDtoRequest updateRequest) {
        PaymentCardDtoResponseWithUser paymentCardDtoResponse = paymentCardService.update(id, updateRequest);
        return ResponseEntity.ok(paymentCardDtoResponse);
    }

    @Override
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        paymentCardService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> updateActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        paymentCardService.updateActiveStatus(id, active);
        return ResponseEntity.noContent().build();
    }
}
