package com.inno.controller.impl;

import com.inno.controller.BaseController;
import com.inno.service.PaymentCardService;
import com.inno.service.UserService;
import com.inno.service.dto.PaymentCardDtoResponse;
import com.inno.service.dto.SearchingRequest;
import com.inno.service.dto.UserDtoRequest;
import com.inno.service.dto.UserDtoResponse;
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

import java.util.List;

import static com.inno.controller.RestConstants.USERS_V1_API_PATH;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping(value = USERS_V1_API_PATH)
public class UserController implements BaseController<UserDtoRequest, UserDtoResponse, Long> {
    private final UserService userService;
    private final PaymentCardService paymentCardService;

    @Autowired
    public UserController(UserService userService, PaymentCardService paymentCardService) {
        this.userService = userService;
        this.paymentCardService = paymentCardService;
    }

    @Override
    @GetMapping
    @ResponseStatus(OK)
    public ResponseEntity<Page<UserDtoResponse>> findAll(
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(sort = "surname", direction = Sort.Direction.ASC) Pageable pageable) {
        SearchingRequest searchingRequest = null;
        if (search != null && !search.isBlank()) {
            searchingRequest = new SearchingRequest(search);
        }
        Page<UserDtoResponse> pageDtoResponse = userService.findAll(searchingRequest, pageable);
        return new ResponseEntity<>(pageDtoResponse, OK);
    }

    @Override
    @GetMapping(value = "/{id}")
    @ResponseStatus(OK)
    public ResponseEntity<UserDtoResponse> findById(@PathVariable Long id) {
        UserDtoResponse userDtoResponse = userService.findById(id);
        return new ResponseEntity<>(userDtoResponse, OK);
    }

    @Override
    @PostMapping
    @ResponseStatus(CREATED)
    public ResponseEntity<UserDtoResponse> create(@RequestBody UserDtoRequest createRequest) {
        UserDtoResponse userDtoResponse = userService.create(createRequest);
        return new ResponseEntity<>(userDtoResponse, CREATED);
    }

    @Override
    @PutMapping(value = "/{id}")
    @ResponseStatus(OK)
    public ResponseEntity<UserDtoResponse> update(@PathVariable Long id, @RequestBody UserDtoRequest updateRequest) {
        UserDtoResponse userDtoResponse = userService.update(id, updateRequest);
        return new ResponseEntity<>(userDtoResponse, OK);
    }

    @Override
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(NO_CONTENT)
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{id}/status")
    @ResponseStatus(NO_CONTENT)
    public ResponseEntity<Void> updateActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        userService.updateActiveStatus(id, active);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/payment-cards")
    @ResponseStatus(OK)
    public ResponseEntity<List<PaymentCardDtoResponse>> readPaymentCardsByUserId(@PathVariable Long id) {
        List<PaymentCardDtoResponse> tagDtoResponseList = paymentCardService.findAllByUserId(id);
        return new ResponseEntity<>(tagDtoResponseList, OK);
    }
}
