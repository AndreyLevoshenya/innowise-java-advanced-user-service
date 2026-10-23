package com.inno.controller.impl;

import com.inno.controller.BaseController;
import com.inno.service.PaymentCardService;
import com.inno.service.UserService;
import com.inno.service.dto.PaymentCardDtoResponseWithUser;
import com.inno.service.dto.SearchingRequest;
import com.inno.service.dto.UserDtoRequest;
import com.inno.service.dto.UserDtoResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

import java.util.List;

import static com.inno.controller.RestConstants.USERS_V1_API_PATH;
import static org.springframework.http.HttpStatus.CREATED;

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
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<UserDtoResponse>> findAll(
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(sort = "surname", direction = Sort.Direction.ASC) Pageable pageable) {
        SearchingRequest searchingRequest = null;
        if (search != null && !search.isBlank()) {
            searchingRequest = new SearchingRequest(search);
        }
        Page<UserDtoResponse> pageDtoResponse = userService.findAll(searchingRequest, pageable);
        return ResponseEntity.ok(pageDtoResponse);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<UserDtoResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = jwt.getClaim("userId");

        UserDtoResponse userDtoResponse = userService.findById(userId);
        return ResponseEntity.ok(userDtoResponse);
    }

    @Override
    @GetMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or #id == authentication.token.claims['userId']")
    public ResponseEntity<UserDtoResponse> findById(@PathVariable Long id) {
        UserDtoResponse userDtoResponse = userService.findById(id);
        return ResponseEntity.ok(userDtoResponse);
    }

    @Override
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SERVICE')")
    public ResponseEntity<UserDtoResponse> create(@Valid @RequestBody UserDtoRequest createRequest) {
        UserDtoResponse userDtoResponse = userService.create(createRequest);
        return ResponseEntity.status(CREATED).body(userDtoResponse);
    }

    @Override
    @PutMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or #id == authentication.token.claims['userId']")
    public ResponseEntity<UserDtoResponse> update(@PathVariable Long id, @Valid @RequestBody UserDtoRequest updateRequest) {
        UserDtoResponse userDtoResponse = userService.update(id, updateRequest);
        return ResponseEntity.ok(userDtoResponse);
    }

    @Override
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> updateActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        userService.updateActiveStatus(id, active);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/cards")
    @PreAuthorize("hasAuthority('ADMIN') or #id == authentication.token.claims['userId']")
    public ResponseEntity<List<PaymentCardDtoResponseWithUser>> readPaymentCardsByUserId(@PathVariable Long id) {
        List<PaymentCardDtoResponseWithUser> tagDtoResponseList = paymentCardService.findAllByUserId(id);
        return ResponseEntity.ok(tagDtoResponseList);
    }

    @GetMapping(value = "/me/cards")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<PaymentCardDtoResponseWithUser>> readPaymentCardsByUserId(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");

        List<PaymentCardDtoResponseWithUser> tagDtoResponseList = paymentCardService.findAllByUserId(userId);
        return ResponseEntity.ok(tagDtoResponseList);
    }
}
