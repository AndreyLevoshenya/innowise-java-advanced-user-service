package com.inno.controller.impl;

import com.inno.repository.PaymentCardRepository;
import com.inno.repository.UserRepository;
import com.inno.repository.model.PaymentCard;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static com.inno.controller.RestConstants.PAYMENT_CARDS_V1_API_PATH;
import static com.inno.controller.RestConstants.USERS_V1_API_PATH;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String BY_ID = PAYMENT_CARDS_V1_API_PATH + "/{id}";
    private static final String STATUS = PAYMENT_CARDS_V1_API_PATH + "/{id}/status";
    private static final long NOT_EXISTING_ID = 999_999L;

    private static final String NUMBER_1 = "4111111111111111";
    private static final String NUMBER_2 = "5555555555554444";
    private static final String NUMBER_3 = "378282246310005";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    private long userId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
        userId = createUserViaApi("Ivan", "Petrov", "ivan@example.com");
    }

    // ---------- helpers ----------

    private long createUserViaApi(String name, String surname, String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("surname", surname);
        body.put("birthDate", "1998-05-20");
        body.put("email", email);
        body.put("active", true);

        return given().contentType(ContentType.JSON).body(body).when().post(USERS_V1_API_PATH).then().log().ifValidationFails().statusCode(201).extract().jsonPath().getLong("id");
    }

    private Map<String, Object> cardBody(long ownerId, String number, String holder) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", ownerId);
        body.put("number", number);
        body.put("holder", holder);
        body.put("expirationDate", LocalDate.now().plusYears(3).toString());
        body.put("active", true);
        return body;
    }

    private long createCardViaApi(long ownerId, String number, String holder) {
        return given().contentType(ContentType.JSON).body(cardBody(ownerId, number, holder)).when().post(PAYMENT_CARDS_V1_API_PATH).then().log().ifValidationFails().statusCode(201).extract().jsonPath().getLong("id");
    }

    // ---------- POST /api/v1/payment-cards ----------

    @Nested
    @DisplayName("POST create")
    class Create {

        @Test
        @DisplayName("creates card and persists it to DB")
        void shouldCreateCard() {
            LocalDate expiration = LocalDate.now().plusYears(3);

            long id = given().contentType(ContentType.JSON).body(cardBody(userId, NUMBER_1, "IVAN PETROV")).when().post(PAYMENT_CARDS_V1_API_PATH).then().log().ifValidationFails().statusCode(201).body("id", notNullValue()).body("number", equalTo(NUMBER_1)).body("holder", equalTo("IVAN PETROV")).body("expirationDate", equalTo(expiration.toString())).body("active", equalTo(true)).body("user.id", equalTo((int) userId)).extract().jsonPath().getLong("id");

            PaymentCard saved = paymentCardRepository.findById(id).orElseThrow();
            assertEquals(NUMBER_1, saved.getNumber());
            assertEquals("IVAN PETROV", saved.getHolder());
            assertEquals(expiration, saved.getExpirationDate());
            assertTrue(saved.isActive());
        }

        @Test
        @DisplayName("returns 404 when user does not exist")
        void shouldReturn404WhenUserNotFound() {
            given().contentType(ContentType.JSON).body(cardBody(NOT_EXISTING_ID, NUMBER_1, "IVAN PETROV")).when().post(PAYMENT_CARDS_V1_API_PATH).then().statusCode(404);

            assertEquals(0, paymentCardRepository.count());
        }

        @Test
        @DisplayName("returns 400 when number is blank")
        void shouldReturn400WhenNumberBlank() {
            given().contentType(ContentType.JSON).body(cardBody(userId, "", "IVAN PETROV")).when().post(PAYMENT_CARDS_V1_API_PATH).then().statusCode(400);

            assertEquals(0, paymentCardRepository.count());
        }

        @Test
        @DisplayName("returns 400 when holder is blank")
        void shouldReturn400WhenHolderBlank() {
            given().contentType(ContentType.JSON).body(cardBody(userId, NUMBER_1, "")).when().post(PAYMENT_CARDS_V1_API_PATH).then().statusCode(400);

            assertEquals(0, paymentCardRepository.count());
        }

        @Test
        @DisplayName("returns 400 when userId is missing")
        void shouldReturn400WhenUserIdMissing() {
            Map<String, Object> body = cardBody(userId, NUMBER_1, "IVAN PETROV");
            body.remove("userId");

            given().contentType(ContentType.JSON).body(body).when().post(PAYMENT_CARDS_V1_API_PATH).then().statusCode(400);

            assertEquals(0, paymentCardRepository.count());
        }

        @Test
        @DisplayName("returns 400 when active is missing")
        void shouldReturn400WhenActiveMissing() {
            Map<String, Object> body = cardBody(userId, NUMBER_1, "IVAN PETROV");
            body.remove("active");

            given().contentType(ContentType.JSON).body(body).when().post(PAYMENT_CARDS_V1_API_PATH).then().statusCode(400);

            assertEquals(0, paymentCardRepository.count());
        }

        @Test
        @DisplayName("returns 400 when expiration date is missing")
        void shouldReturn400WhenExpirationDateMissing() {
            Map<String, Object> body = cardBody(userId, NUMBER_1, "IVAN PETROV");
            body.remove("expirationDate");

            given().contentType(ContentType.JSON).body(body).when().post(PAYMENT_CARDS_V1_API_PATH).then().statusCode(400);

            assertEquals(0, paymentCardRepository.count());
        }

        @Test
        @DisplayName("returns 400 when body is empty")
        void shouldReturn400WhenBodyEmpty() {
            given().contentType(ContentType.JSON).body("{}").when().post(PAYMENT_CARDS_V1_API_PATH).then().statusCode(400);
        }
    }

    // ---------- GET /api/v1/payment-cards/{id} ----------

    @Nested
    @DisplayName("GET findById")
    class FindById {

        @Test
        @DisplayName("returns card with user by id")
        void shouldReturnCard() {
            long id = createCardViaApi(userId, NUMBER_1, "IVAN PETROV");

            given().when().get(BY_ID, id).then().statusCode(200).body("id", equalTo((int) id)).body("number", equalTo(NUMBER_1)).body("holder", equalTo("IVAN PETROV")).body("user.id", equalTo((int) userId));
        }

        @Test
        @DisplayName("returns 404 when card does not exist")
        void shouldReturn404() {
            given().when().get(BY_ID, NOT_EXISTING_ID).then().statusCode(404);
        }
    }

    // ---------- GET /api/v1/payment-cards ----------

    @Nested
    @DisplayName("GET findAll")
    class FindAll {

        @Test
        @DisplayName("returns empty page when there are no cards")
        void shouldReturnEmptyPage() {
            given().when().get(PAYMENT_CARDS_V1_API_PATH).then().statusCode(200).body("content", empty()).body("totalElements", equalTo(0));
        }

        @Test
        @DisplayName("returns cards sorted by number ASC by default")
        void shouldReturnSortedByNumberByDefault() {
            createCardViaApi(userId, NUMBER_2, "IVAN PETROV");
            createCardViaApi(userId, NUMBER_3, "IVAN PETROV");
            createCardViaApi(userId, NUMBER_1, "IVAN PETROV");

            given().when().get(PAYMENT_CARDS_V1_API_PATH).then().statusCode(200).body("totalElements", equalTo(3)).body("content.number", contains(NUMBER_3, NUMBER_1, NUMBER_2));
        }

        @Test
        @DisplayName("supports pagination")
        void shouldPaginate() {
            createCardViaApi(userId, NUMBER_1, "IVAN PETROV");
            createCardViaApi(userId, NUMBER_2, "IVAN PETROV");
            createCardViaApi(userId, NUMBER_3, "IVAN PETROV");

            given().queryParam("page", 1).queryParam("size", 2).when().get(PAYMENT_CARDS_V1_API_PATH).then().statusCode(200).body("content", hasSize(1)).body("totalElements", equalTo(3)).body("totalPages", equalTo(2));
        }

        @Test
        @DisplayName("supports custom sorting")
        void shouldSortByCustomField() {
            createCardViaApi(userId, NUMBER_1, "ANNA SIDOROVA");
            createCardViaApi(userId, NUMBER_2, "BORIS ABRAMOV");

            given().queryParam("sort", "holder,desc").when().get(PAYMENT_CARDS_V1_API_PATH).then().statusCode(200).body("content.holder", contains("BORIS ABRAMOV", "ANNA SIDOROVA"));
        }

        @Test
        @DisplayName("filters cards by search param")
        void shouldFilterBySearch() {
            createCardViaApi(userId, NUMBER_1, "IVAN PETROV");
            createCardViaApi(userId, NUMBER_2, "ANNA SIDOROVA");

            given().queryParam("search", "ANNA").when().get(PAYMENT_CARDS_V1_API_PATH).then().statusCode(200).body("content", hasSize(1)).body("content[0].holder", equalTo("ANNA SIDOROVA"));
        }

        @Test
        @DisplayName("blank search param is ignored")
        void shouldIgnoreBlankSearch() {
            createCardViaApi(userId, NUMBER_1, "IVAN PETROV");
            createCardViaApi(userId, NUMBER_2, "ANNA SIDOROVA");

            given().queryParam("search", "   ").when().get(PAYMENT_CARDS_V1_API_PATH).then().statusCode(200).body("content", hasSize(2));
        }
    }

    // ---------- PUT /api/v1/payment-cards/{id} ----------

    @Nested
    @DisplayName("PUT update")
    class Update {

        @Test
        @DisplayName("updates card and persists changes to DB")
        void shouldUpdateCard() {
            long id = createCardViaApi(userId, NUMBER_1, "IVAN PETROV");

            LocalDate newExpiration = LocalDate.now().plusYears(5);
            Map<String, Object> updated = cardBody(userId, NUMBER_2, "PETR IVANOV");
            updated.put("expirationDate", newExpiration.toString());

            given().contentType(ContentType.JSON).body(updated).when().put(BY_ID, id).then().log().ifValidationFails().statusCode(200).body("id", equalTo((int) id)).body("number", equalTo(NUMBER_2)).body("holder", equalTo("PETR IVANOV")).body("expirationDate", equalTo(newExpiration.toString()));

            PaymentCard fromDb = paymentCardRepository.findById(id).orElseThrow();
            assertEquals(NUMBER_2, fromDb.getNumber());
            assertEquals("PETR IVANOV", fromDb.getHolder());
            assertEquals(newExpiration, fromDb.getExpirationDate());
        }

        @Test
        @DisplayName("returns 404 when card does not exist")
        void shouldReturn404() {
            given().contentType(ContentType.JSON).body(cardBody(userId, NUMBER_2, "PETR IVANOV")).when().put(BY_ID, NOT_EXISTING_ID).then().statusCode(404);
        }

        @Test
        @DisplayName("returns 400 on invalid body and leaves DB unchanged")
        void shouldReturn400OnInvalidBody() {
            long id = createCardViaApi(userId, NUMBER_1, "IVAN PETROV");

            given().contentType(ContentType.JSON).body(cardBody(userId, "", "")).when().put(BY_ID, id).then().statusCode(400);

            PaymentCard fromDb = paymentCardRepository.findById(id).orElseThrow();
            assertEquals(NUMBER_1, fromDb.getNumber());
            assertEquals("IVAN PETROV", fromDb.getHolder());
        }
    }

    // ---------- DELETE /api/v1/payment-cards/{id} ----------

    @Nested
    @DisplayName("DELETE deleteById")
    class Delete {

        @Test
        @DisplayName("deletes card from DB but keeps the user")
        void shouldDeleteCardAndKeepUser() {
            long id = createCardViaApi(userId, NUMBER_1, "IVAN PETROV");

            given().when().delete(BY_ID, id).then().statusCode(204);

            assertFalse(paymentCardRepository.existsById(id));
            assertTrue(userRepository.existsById(userId));
        }

        @Test
        @DisplayName("deletes only the requested card")
        void shouldDeleteOnlyRequestedCard() {
            long id1 = createCardViaApi(userId, NUMBER_1, "IVAN PETROV");
            long id2 = createCardViaApi(userId, NUMBER_2, "IVAN PETROV");

            given().when().delete(BY_ID, id1).then().statusCode(204);

            assertFalse(paymentCardRepository.existsById(id1));
            assertTrue(paymentCardRepository.existsById(id2));
        }

        @Test
        @DisplayName("returns 404 when card does not exist")
        void shouldReturn404() {
            given().when().delete(BY_ID, NOT_EXISTING_ID).then().statusCode(404);
        }
    }

    // ---------- PATCH /api/v1/payment-cards/{id}/status ----------

    @Nested
    @DisplayName("PATCH updateActiveStatus")
    class UpdateActiveStatus {

        @Test
        @DisplayName("deactivates and activates card")
        void shouldToggleActiveStatus() {
            long id = createCardViaApi(userId, NUMBER_1, "IVAN PETROV");

            given().queryParam("active", false).when().patch(STATUS, id).then().statusCode(204);
            assertFalse(paymentCardRepository.findById(id).orElseThrow().isActive());

            given().queryParam("active", true).when().patch(STATUS, id).then().statusCode(204);
            assertTrue(paymentCardRepository.findById(id).orElseThrow().isActive());
        }

        @Test
        @DisplayName("returns 404 when card does not exist")
        void shouldReturn404() {
            given().queryParam("active", true).when().patch(STATUS, NOT_EXISTING_ID).then().statusCode(404);
        }

        @Test
        @DisplayName("returns 400 when 'active' param is missing")
        void shouldReturn400WhenParamMissing() {
            long id = createCardViaApi(userId, NUMBER_1, "IVAN PETROV");

            given().when().patch(STATUS, id).then().statusCode(400);
        }

        @Test
        @DisplayName("returns 400 when 'active' param is not boolean")
        void shouldReturn400WhenParamInvalid() {
            long id = createCardViaApi(userId, NUMBER_1, "IVAN PETROV");

            given().queryParam("active", "maybe").when().patch(STATUS, id).then().statusCode(400);
        }
    }
}