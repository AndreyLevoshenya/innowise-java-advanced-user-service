package com.inno.controller.impl;

import com.inno.repository.PaymentCardRepository;
import com.inno.repository.UserRepository;
import com.inno.repository.model.PaymentCard;
import com.inno.repository.model.User;
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

import static com.inno.controller.RestConstants.USERS_V1_API_PATH;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String BY_ID = USERS_V1_API_PATH + "/{id}";
    private static final String STATUS = USERS_V1_API_PATH + "/{id}/status";
    private static final String CARDS = USERS_V1_API_PATH + "/{id}/payment-cards";
    private static final long NOT_EXISTING_ID = 999_999L;

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- helpers ----------

    private Map<String, Object> userBody(String name, String surname, String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("surname", surname);
        body.put("birthDate", "1998-05-20");
        body.put("email", email);
        body.put("active", true);
        return body;
    }

    private long createUserViaApi(String name, String surname, String email) {
        return given()
                .contentType(ContentType.JSON)
                .body(userBody(name, surname, email))
                .when()
                .post(USERS_V1_API_PATH)
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private PaymentCard createCardInDb(long userId, String number, String holder) {
        User user = userRepository.findById(userId).orElseThrow();
        PaymentCard card = new PaymentCard();
        card.setUser(user);
        card.setNumber(number);
        card.setHolder(holder);
        card.setExpirationDate(LocalDate.now().plusYears(3));
        card.setActive(true);
        return paymentCardRepository.save(card);
    }

    // ---------- POST /api/v1/users ----------

    @Nested
    @DisplayName("POST create")
    class Create {

        @Test
        @DisplayName("creates user and persists it to DB")
        void shouldCreateUser() {
            long id = given()
                    .contentType(ContentType.JSON)
                    .body(userBody("Ivan", "Petrov", "ivan.petrov@example.com"))
                    .when()
                    .post(USERS_V1_API_PATH)
                    .then()
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("name", equalTo("Ivan"))
                    .body("surname", equalTo("Petrov"))
                    .body("email", equalTo("ivan.petrov@example.com"))
                    .body("birthDate", equalTo("1998-05-20"))
                    .extract()
                    .jsonPath()
                    .getLong("id");

            User saved = userRepository.findById(id).orElseThrow();
            assertEquals("Ivan", saved.getName());
            assertEquals("Petrov", saved.getSurname());
            assertEquals("ivan.petrov@example.com", saved.getEmail());
            assertEquals(LocalDate.of(1998, 5, 20), saved.getBirthDate());
        }

        @Test
        @DisplayName("returns 400 when name is blank")
        void shouldReturn400WhenNameBlank() {
            given()
                    .contentType(ContentType.JSON)
                    .body(userBody("", "Petrov", "ivan@example.com"))
                    .when()
                    .post(USERS_V1_API_PATH)
                    .then()
                    .statusCode(400);

            assertEquals(0, userRepository.count());
        }

        @Test
        @DisplayName("returns 400 when email is invalid")
        void shouldReturn400WhenEmailInvalid() {
            given()
                    .contentType(ContentType.JSON)
                    .body(userBody("Ivan", "Petrov", "not-an-email"))
                    .when()
                    .post(USERS_V1_API_PATH)
                    .then()
                    .statusCode(400);

            assertEquals(0, userRepository.count());
        }

        @Test
        @DisplayName("returns 400 when body is empty")
        void shouldReturn400WhenBodyEmpty() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .when()
                    .post(USERS_V1_API_PATH)
                    .then()
                    .statusCode(400);
        }
    }

    // ---------- GET /api/v1/users/{id} ----------

    @Nested
    @DisplayName("GET findById")
    class FindById {

        @Test
        @DisplayName("returns user by id")
        void shouldReturnUser() {
            long id = createUserViaApi("Ivan", "Petrov", "ivan@example.com");

            given()
                    .when()
                    .get(BY_ID, id)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo((int) id))
                    .body("name", equalTo("Ivan"))
                    .body("surname", equalTo("Petrov"))
                    .body("email", equalTo("ivan@example.com"));
        }

        @Test
        @DisplayName("returns 404 when user does not exist")
        void shouldReturn404() {
            given()
                    .when()
                    .get(BY_ID, NOT_EXISTING_ID)
                    .then()
                    .statusCode(404);
        }
    }

    // ---------- GET /api/v1/users ----------

    @Nested
    @DisplayName("GET findAll")
    class FindAll {

        @Test
        @DisplayName("returns empty page when there are no users")
        void shouldReturnEmptyPage() {
            given()
                    .when()
                    .get(USERS_V1_API_PATH)
                    .then()
                    .statusCode(200)
                    .body("content", empty())
                    .body("totalElements", equalTo(0));
        }

        @Test
        @DisplayName("returns users sorted by surname ASC by default")
        void shouldReturnSortedBySurnameByDefault() {
            createUserViaApi("Anna", "Zaitseva", "anna@example.com");
            createUserViaApi("Boris", "Abramov", "boris@example.com");
            createUserViaApi("Vera", "Morozova", "vera@example.com");

            given()
                    .when()
                    .get(USERS_V1_API_PATH)
                    .then()
                    .statusCode(200)
                    .body("totalElements", equalTo(3))
                    .body("content.surname", contains("Abramov", "Morozova", "Zaitseva"));
        }

        @Test
        @DisplayName("supports pagination")
        void shouldPaginate() {
            createUserViaApi("Anna", "Aaa", "a@example.com");
            createUserViaApi("Boris", "Bbb", "b@example.com");
            createUserViaApi("Vera", "Ccc", "c@example.com");

            given()
                    .queryParam("page", 1)
                    .queryParam("size", 2)
                    .when()
                    .get(USERS_V1_API_PATH)
                    .then()
                    .statusCode(200)
                    .body("content", hasSize(1))
                    .body("content[0].surname", equalTo("Ccc"))
                    .body("totalElements", equalTo(3))
                    .body("totalPages", equalTo(2));
        }

        @Test
        @DisplayName("supports custom sorting")
        void shouldSortByCustomField() {
            createUserViaApi("Anna", "Aaa", "a@example.com");
            createUserViaApi("Boris", "Bbb", "b@example.com");

            given()
                    .queryParam("sort", "name,desc")
                    .when()
                    .get(USERS_V1_API_PATH)
                    .then()
                    .statusCode(200)
                    .body("content.name", contains("Boris", "Anna"));
        }

        @Test
        @DisplayName("filters users by search param")
        void shouldFilterBySearch() {
            createUserViaApi("Ivan", "Petrov", "ivan@example.com");
            createUserViaApi("Anna", "Sidorova", "anna@example.com");

            given()
                    .queryParam("search", "Ivan")
                    .when()
                    .get(USERS_V1_API_PATH)
                    .then()
                    .statusCode(200)
                    .body("content", hasSize(1))
                    .body("content[0].name", equalTo("Ivan"));
        }

        @Test
        @DisplayName("blank search param is ignored")
        void shouldIgnoreBlankSearch() {
            createUserViaApi("Ivan", "Petrov", "ivan@example.com");
            createUserViaApi("Anna", "Sidorova", "anna@example.com");

            given()
                    .queryParam("search", "   ")
                    .when()
                    .get(USERS_V1_API_PATH)
                    .then()
                    .statusCode(200)
                    .body("content", hasSize(2));
        }
    }

    // ---------- PUT /api/v1/users/{id} ----------

    @Nested
    @DisplayName("PUT update")
    class Update {

        @Test
        @DisplayName("updates user and persists changes to DB")
        void shouldUpdateUser() {
            long id = createUserViaApi("Ivan", "Petrov", "ivan@example.com");

            Map<String, Object> updated = userBody("Petr", "Ivanov", "petr.ivanov@example.com");
            updated.put("birthDate", "2000-01-15");

            given()
                    .contentType(ContentType.JSON)
                    .body(updated)
                    .when()
                    .put(BY_ID, id)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo((int) id))
                    .body("name", equalTo("Petr"))
                    .body("surname", equalTo("Ivanov"))
                    .body("email", equalTo("petr.ivanov@example.com"))
                    .body("birthDate", equalTo("2000-01-15"));

            User fromDb = userRepository.findById(id).orElseThrow();
            assertEquals("Petr", fromDb.getName());
            assertEquals("Ivanov", fromDb.getSurname());
            assertEquals("petr.ivanov@example.com", fromDb.getEmail());
            assertEquals(LocalDate.of(2000, 1, 15), fromDb.getBirthDate());
        }

        @Test
        @DisplayName("returns 404 when user does not exist")
        void shouldReturn404() {
            given()
                    .contentType(ContentType.JSON)
                    .body(userBody("Petr", "Ivanov", "petr@example.com"))
                    .when()
                    .put(BY_ID, NOT_EXISTING_ID)
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("returns 400 on invalid body and leaves DB unchanged")
        void shouldReturn400OnInvalidBody() {
            long id = createUserViaApi("Ivan", "Petrov", "ivan@example.com");

            given()
                    .contentType(ContentType.JSON)
                    .body(userBody("", "Ivanov", "bad-email"))
                    .when()
                    .put(BY_ID, id)
                    .then()
                    .statusCode(400);

            User fromDb = userRepository.findById(id).orElseThrow();
            assertEquals("Ivan", fromDb.getName());
            assertEquals("ivan@example.com", fromDb.getEmail());
        }
    }

    // ---------- DELETE /api/v1/users/{id} ----------

    @Nested
    @DisplayName("DELETE deleteById")
    class Delete {

        @Test
        @DisplayName("deletes user from DB")
        void shouldDeleteUser() {
            long id = createUserViaApi("Ivan", "Petrov", "ivan@example.com");

            given()
                    .when()
                    .delete(BY_ID, id)
                    .then()
                    .statusCode(204);

            assertFalse(userRepository.existsById(id));
        }

        @Test
        @DisplayName("cascades deletion to payment cards")
        void shouldCascadeDeleteCards() {
            long id = createUserViaApi("Ivan", "Petrov", "ivan@example.com");
            createCardInDb(id, "4111111111111111", "IVAN PETROV");
            createCardInDb(id, "5555555555554444", "IVAN PETROV");
            assertEquals(2, paymentCardRepository.count());

            given()
                    .when()
                    .delete(BY_ID, id)
                    .then()
                    .statusCode(204);

            assertFalse(userRepository.existsById(id));
            assertEquals(0, paymentCardRepository.count());
        }

        @Test
        @DisplayName("returns 404 when user does not exist")
        void shouldReturn404() {
            given()
                    .when()
                    .delete(BY_ID, NOT_EXISTING_ID)
                    .then()
                    .statusCode(404);
        }
    }

    // ---------- PATCH /api/v1/users/{id}/status ----------

    @Nested
    @DisplayName("PATCH updateActiveStatus")
    class UpdateActiveStatus {

        @Test
        @DisplayName("deactivates and activates user")
        void shouldToggleActiveStatus() {
            long id = createUserViaApi("Ivan", "Petrov", "ivan@example.com");

            given()
                    .queryParam("active", false)
                    .when()
                    .patch(STATUS, id)
                    .then()
                    .statusCode(204);
            assertFalse(userRepository.findById(id).orElseThrow().isActive());

            given()
                    .queryParam("active", true)
                    .when()
                    .patch(STATUS, id)
                    .then()
                    .statusCode(204);
            assertTrue(userRepository.findById(id).orElseThrow().isActive());
        }

        @Test
        @DisplayName("returns 404 when user does not exist")
        void shouldReturn404() {
            given()
                    .queryParam("active", true)
                    .when()
                    .patch(STATUS, NOT_EXISTING_ID)
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("returns 400 when 'active' param is missing")
        void shouldReturn400WhenParamMissing() {
            long id = createUserViaApi("Ivan", "Petrov", "ivan@example.com");

            given()
                    .when()
                    .patch(STATUS, id)
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("returns 400 when 'active' param is not boolean")
        void shouldReturn400WhenParamInvalid() {
            long id = createUserViaApi("Ivan", "Petrov", "ivan@example.com");

            given()
                    .queryParam("active", "maybe")
                    .when()
                    .patch(STATUS, id)
                    .then()
                    .statusCode(400);
        }
    }

    // ---------- GET /api/v1/users/{id}/payment-cards ----------

    @Nested
    @DisplayName("GET readPaymentCardsByUserId")
    class ReadPaymentCards {

        @Test
        @DisplayName("returns only cards of the requested user")
        void shouldReturnCardsOfUser() {
            long ivanId = createUserViaApi("Ivan", "Petrov", "ivan@example.com");
            long annaId = createUserViaApi("Anna", "Sidorova", "anna@example.com");
            createCardInDb(ivanId, "4111111111111111", "IVAN PETROV");
            createCardInDb(ivanId, "5555555555554444", "IVAN PETROV");
            createCardInDb(annaId, "378282246310005", "ANNA SIDOROVA");

            given()
                    .when()
                    .get(CARDS, ivanId)
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(2))
                    .body("holder", contains("IVAN PETROV", "IVAN PETROV"));
        }

        @Test
        @DisplayName("returns empty list when user has no cards")
        void shouldReturnEmptyList() {
            long id = createUserViaApi("Ivan", "Petrov", "ivan@example.com");

            given()
                    .when()
                    .get(CARDS, id)
                    .then()
                    .statusCode(200)
                    .body("$", empty());
        }
    }
}