package com.inno.controller.impl;

import com.inno.repository.PaymentCardRepository;
import com.inno.repository.UserRepository;
import com.inno.repository.model.PaymentCard;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static com.inno.controller.RestConstants.PAYMENT_CARDS_V1_API_PATH;
import static com.inno.controller.RestConstants.USERS_V1_API_PATH;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {
    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaymentCardRepository paymentCardRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = PAYMENT_CARDS_V1_API_PATH;

        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Long createUser(String email) {
        return given()
                .basePath(USERS_V1_API_PATH)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "Ivan",
                        "surname", "Petrov",
                        "birthDate", "2000-01-15",
                        "email", email
                ))
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private Map<String, Object> validCardRequest(
            Long userId, String number) {
        return Map.of(
                "number", number,
                "holder", "Ivan Petrov",
                "expirationDate", "2030-12-31",
                "userId", userId
        );
    }

    private Long createCard(Long userId, String number) {
        return given()
                .contentType(ContentType.JSON)
                .body(validCardRequest(userId, number))
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getLong("id");
    }

    @Test
    void createCard_shouldPersistCard() {
        Long userId = createUser("card.create@test.com");

        Long cardId = createCard(userId, "1111222233334444");

        PaymentCard saved = paymentCardRepository.findById(cardId).orElseThrow();

        assertEquals("1111222233334444", saved.getNumber());
        assertEquals("Ivan Petrov", saved.getHolder());
    }

    @Test
    void createCard_withInvalidRequest_shouldReturn400() {
        Long userId = createUser("card.invalid@test.com");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "number", "",
                        "holder", "",
                        "expirationDate", "invalid-date",
                        "userId", userId
                ))
                .when()
                .post()
                .then()
                .statusCode(400);

        assertEquals(0L, paymentCardRepository.count());
    }

    @Test
    void findById_shouldReturnCard() {
        Long userId = createUser("card.get@test.com");
        Long cardId = createCard(userId, "1111222233334444");

        given()
                .when()
                .get("/{id}", cardId)
                .then()
                .statusCode(200)
                .body("id", equalTo(cardId.intValue()))
                .body("number", equalTo("1111222233334444"));
    }

    @Test
    void findById_whenCardDoesNotExist_shouldReturn404() {
        given()
                .when()
                .get("/{id}", 999999L)
                .then()
                .statusCode(404);
    }

    @Test
    void findAll_shouldReturnPaginatedCards() {
        Long userId = createUser("card.page@test.com");

        createCard(userId, "1111222233334444");
        createCard(userId, "5555666677778888");

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("totalElements", equalTo(2));
    }

    @Test
    void findAll_withSearch_shouldFilterCards() {
        Long userId = createUser("card.search@test.com");

        createCard(userId, "1111222233334444");
        createCard(userId, "5555666677778888");

        given()
                .queryParam("search", "1111")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].number",
                        equalTo("1111222233334444"));
    }

    @Test
    void update_shouldModifyCard() {
        Long userId = createUser("card.update@test.com");
        Long cardId = createCard(userId, "1111222233334444");

        given()
                .contentType(ContentType.JSON)
                .body(validCardRequest(userId, "5555666677778888"))
                .when()
                .put("/{id}", cardId)
                .then()
                .statusCode(200)
                .body("id", equalTo(cardId.intValue()))
                .body("number", equalTo("5555666677778888"));

        assertEquals(
                "5555666677778888",
                paymentCardRepository.findById(cardId).orElseThrow().getNumber()
        );
    }

    @Test
    void updateActiveStatus_shouldDeactivateCard() {
        Long userId = createUser("card.status@test.com");
        Long cardId = createCard(userId, "1111222233334444");

        given()
                .queryParam("active", false)
                .when()
                .patch("/{id}/status", cardId)
                .then()
                .statusCode(204);

        assertFalse(
                paymentCardRepository.findById(cardId).orElseThrow().isActive()
        );
    }

    @Test
    void updateActiveStatus_whenAlreadyActive_shouldRemainActive() {
        Long userId = createUser("card.reactivate@test.com");
        Long cardId = createCard(userId, "1111222233334444");

        given()
                .queryParam("active", true)
                .when()
                .patch("/{id}/status", cardId)
                .then()
                .statusCode(204);

        given()
                .queryParam("active", true)
                .when()
                .patch("/{id}/status", cardId)
                .then()
                .statusCode(204);

        assertTrue(
                paymentCardRepository.findById(cardId).orElseThrow().isActive()
        );
    }

    @Test
    void deleteById_shouldRemoveCard() {
        Long userId = createUser("card.delete@test.com");
        Long cardId = createCard(userId, "1111222233334444");

        given()
                .when()
                .delete("/{id}", cardId)
                .then()
                .statusCode(204);

        assertFalse(paymentCardRepository.existsById(cardId));
    }

    @Test
    void createSixthCard_shouldBeRejected() {
        Long userId = createUser("card.limit@test.com");

        for (int i = 1; i <= 5; i++) {
            createCard(userId, String.format("%016d", i));
        }

        given()
                .contentType(ContentType.JSON)
                .body(validCardRequest(userId, "9999999999999999"))
                .when()
                .post()
                .then()
                .statusCode(409);

        assertEquals(5L, paymentCardRepository.count());
    }
}

