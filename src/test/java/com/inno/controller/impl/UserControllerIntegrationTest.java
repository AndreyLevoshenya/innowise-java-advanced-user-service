package com.inno.controller.impl;

import com.inno.repository.UserRepository;
import com.inno.repository.model.User;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static com.inno.controller.RestConstants.USERS_V1_API_PATH;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = USERS_V1_API_PATH;

        userRepository.deleteAll();
    }

    private Map<String, Object> validUserRequest(
            String name, String surname, String email) {
        return Map.of(
                "name", name,
                "surname", surname,
                "birthDate", "2000-01-15",
                "email", email
        );
    }

    private Long createUser(String name, String surname, String email) {
        return given()
                .contentType(ContentType.JSON)
                .body(validUserRequest(name, surname, email))
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
    void createUser_shouldPersistUserInDatabase() {
        Long id = createUser(
                "Ivan", "Petrov", "ivan.petrov@test.com");

        // Verify persistence in the actual database.
        User savedUser = userRepository.findById(id).orElseThrow();

        assertEquals("Ivan", savedUser.getName());
        assertEquals("Petrov", savedUser.getSurname());
        assertEquals("ivan.petrov@test.com", savedUser.getEmail());
    }

    @Test
    void createUser_withInvalidRequest_shouldReturn400() {
        Map<String, Object> request = Map.of(
                "name", "",
                "surname", "Petrov",
                "birthDate", "2000-01-15",
                "email", "invalid-email"
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400);

        assertEquals(0L, userRepository.count());
    }

    @Test
    void createUser_withDuplicateEmail_shouldReturn409() {
        String email = "duplicate@test.com";

        createUser("Ivan", "Petrov", email);

        given()
                .contentType(ContentType.JSON)
                .body(validUserRequest("Petr", "Sidorov", email))
                .when()
                .post()
                .then()
                .statusCode(409);

        assertEquals(
                1L,
                userRepository.count()
        );
    }

    @Test
    void findById_shouldReturnExistingUser() {
        Long id = createUser(
                "Ivan", "Petrov", "ivan.get@test.com");

        given()
                .when()
                .get("/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.intValue()))
                .body("name", equalTo("Ivan"))
                .body("surname", equalTo("Petrov"))
                .body("email", equalTo("ivan.get@test.com"));
    }

    @Test
    void findById_whenUserDoesNotExist_shouldReturn404() {
        given()
                .when()
                .get("/{id}", 999999L)
                .then()
                .statusCode(404);
    }

    @Test
    void findAll_shouldReturnPaginatedUsers() {
        createUser("Ivan", "Petrov", "ivan.page@test.com");
        createUser("Petr", "Sidorov", "petr.page@test.com");

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
    void findAll_withSearch_shouldFilterUsers() {
        createUser("Ivan", "Petrov", "ivan.search1@test.com");
        createUser("Ivan", "Sidorov", "ivan.search2@test.com");
        createUser("Petr", "Ivanov", "petr.search@test.com");

        given()
                .queryParam("search", "Ivan")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(3));
    }

    @Test
    void update_shouldModifyExistingUser() {
        Long id = createUser(
                "Ivan", "Petrov", "ivan.update@test.com");

        given()
                .contentType(ContentType.JSON)
                .body(validUserRequest(
                        "Alex", "Smirnov", "alex.update@test.com"))
                .when()
                .put("/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id.intValue()))
                .body("name", equalTo("Alex"))
                .body("surname", equalTo("Smirnov"))
                .body("email", equalTo("alex.update@test.com"));

        User updatedUser = userRepository.findById(id).orElseThrow();
        assertEquals("Alex", updatedUser.getName());
    }

    @Test
    void updateActiveStatus_shouldDeactivateUser() {
        Long id = createUser(
                "Ivan", "Petrov", "ivan.status@test.com");

        given()
                .queryParam("active", false)
                .when()
                .patch("/{id}/status", id)
                .then()
                .statusCode(204);

        User user = userRepository.findById(id).orElseThrow();
        assertFalse(user.isActive());
    }

    @Test
    void deleteById_shouldRemoveUser() {
        Long id = createUser(
                "Ivan", "Petrov", "ivan.delete@test.com");

        given()
                .when()
                .delete("/{id}", id)
                .then()
                .statusCode(204);

        assertFalse(userRepository.existsById(id));
    }

    private void insertCard(
            Long userId,
            String number,
            String holder,
            String expirationDate
    ) {
        jdbcTemplate.update("""
                        INSERT INTO payment_cards
                            (user_id, number, holder, expiration_date, active,
                             created_at, updated_at)
                        VALUES (?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                userId, number, holder, expirationDate
        );
    }

    @Test
    void updateActiveStatus_whenUserAlreadyActive_shouldReturn204() {
        Long id = createUser(
                "Ivan", "Petrov", "ivan.reactivate@test.com");

        // First activation
        given()
                .queryParam("active", true)
                .when()
                .patch("/{id}/status", id)
                .then()
                .statusCode(204);

        // Repeated activation
        given()
                .queryParam("active", true)
                .when()
                .patch("/{id}/status", id)
                .then()
                .statusCode(204);

        User user = userRepository.findById(id).orElseThrow();
        assertTrue(user.isActive());
    }

    @Test
    void findPaymentCardsByUserId_shouldReturnOnlyUsersCards() {
        Long firstUserId = createUser(
                "Ivan", "Petrov", "ivan.cards@test.com");

        Long secondUserId = createUser(
                "Petr", "Sidorov", "petr.cards@test.com");

        insertCard(firstUserId, "1111222233334444",
                "Ivan Petrov", "2030-12-31");
        insertCard(firstUserId, "5555666677778888",
                "Ivan Petrov", "2031-12-31");
        insertCard(secondUserId, "9999000011112222",
                "Petr Sidorov", "2032-12-31");

        given()
                .when()
                .get("/{id}/payment-cards", firstUserId)
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("number",
                        hasItem("1111222233334444"))
                .body("number",
                        hasItem("5555666677778888"));
    }

    @Test
    void deleteUserWithPaymentCards_shouldDeleteUserAndCards() {
        Long userId = createUser(
                "Ivan", "Petrov", "ivan.delete.cards@test.com");

        insertCard(userId, "1111222233334444",
                "Ivan Petrov", "2030-12-31");
        insertCard(userId, "5555666677778888",
                "Ivan Petrov", "2031-12-31");

        assertEquals(
                2,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM payment_cards WHERE user_id = ?",
                        Integer.class,
                        userId
                )
        );

        given()
                .when()
                .delete("/{id}", userId)
                .then()
                .statusCode(204);

        assertFalse(userRepository.existsById(userId));

        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM payment_cards WHERE user_id = ?",
                        Integer.class,
                        userId
                )
        );
    }
}
