package com.santilugani.fulfillmentorchestrator.orders.api;

import com.santilugani.fulfillmentorchestrator.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcTemplate.update("delete from orders");
    }

    @Test
    void createsOrderAndPersistsIt() throws Exception {
        UUID sellerId = UUID.randomUUID();

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sellerId\":\"%s\"}".formatted(sellerId)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        UUID orderId = extractOrderId(result.getResponse().getContentAsString());

        Map<String, Object> persistedOrder = jdbcTemplate.queryForMap(
                "select id, seller_id, status, created_at, updated_at from orders where id = ?",
                orderId
        );

        assertEquals(orderId, persistedOrder.get("id"));
        assertEquals(sellerId, persistedOrder.get("seller_id"));
        assertEquals("CREATED", persistedOrder.get("status"));
        assertNotNull(persistedOrder.get("created_at"));
        assertNotNull(persistedOrder.get("updated_at"));
    }

    @Test
    void rejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sellerId\":\"not-a-uuid\"}"))
                .andExpect(status().isBadRequest());

        Integer orderCount = jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);
        assertEquals(0, orderCount);
    }

    @Test
    void retrievesExistingOrderById() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void returnsNotFoundWhenOrderDoesNotExist() throws Exception {
        UUID missingOrderId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/orders/{id}", missingOrderId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order was not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/%s".formatted(missingOrderId)))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void returnsBadRequestWhenOrderIdIsNotAValidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_ID"))
                .andExpect(jsonPath("$.message").value("Order id must be a valid UUID"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/not-a-uuid"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void cancelsExistingOrder() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);
        Timestamp originalUpdatedAt = jdbcTemplate.queryForObject(
                "select updated_at from orders where id = ?",
                Timestamp.class,
                orderId
        );

        Thread.sleep(20);

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Map<String, Object> persistedOrder = jdbcTemplate.queryForMap(
                "select status, updated_at from orders where id = ?",
                orderId
        );

        assertEquals("CANCELLED", persistedOrder.get("status"));
        Timestamp updatedAtAfterCancellation = (Timestamp) persistedOrder.get("updated_at");
        assertTrue(updatedAtAfterCancellation.toInstant().isAfter(originalUpdatedAt.toInstant()));
    }

    @Test
    void returnsNotFoundWhenCancellingMissingOrder() throws Exception {
        UUID missingOrderId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", missingOrderId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order was not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/%s/cancel".formatted(missingOrderId)))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void returnsBadRequestWhenCancellingWithInvalidOrderId() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_ID"))
                .andExpect(jsonPath("$.message").value("Order id must be a valid UUID"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/not-a-uuid/cancel"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void returnsConflictWhenCancellationIsNotAllowed() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                "insert into orders (id, seller_id, status, created_at, updated_at) values (?, ?, ?, ?, ?)",
                orderId,
                sellerId,
                "DELIVERED",
                Timestamp.from(now),
                Timestamp.from(now)
        );

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.message").value("Cannot transition order status from DELIVERED to CANCELLED"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/%s/cancel".formatted(orderId)))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        String persistedStatus = jdbcTemplate.queryForObject(
                "select status from orders where id = ?",
                String.class,
                orderId
        );
        assertEquals("DELIVERED", persistedStatus);
    }

    private UUID extractOrderId(String responseBody) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(responseBody);
        assertTrue(matcher.find(), "response body should contain an id");
        return UUID.fromString(matcher.group(1));
    }

    private UUID createOrder(UUID sellerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sellerId\":\"%s\"}".formatted(sellerId)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractOrderId(result.getResponse().getContentAsString());
    }
}
