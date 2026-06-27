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
import org.springframework.test.web.servlet.ResultActions;
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

        assertOrderApiError(
                mockMvc.perform(get("/api/v1/orders/{id}", missingOrderId)),
                404,
                "ORDER_NOT_FOUND",
                "Order was not found",
                "/api/v1/orders/%s".formatted(missingOrderId)
        );
    }

    @Test
    void returnsBadRequestWhenOrderIdIsNotAValidUuid() throws Exception {
        assertOrderApiError(
                mockMvc.perform(get("/api/v1/orders/{id}", "not-a-uuid")),
                400,
                "INVALID_ORDER_ID",
                "Order id must be a valid UUID",
                "/api/v1/orders/not-a-uuid"
        );
    }

    @Test
    void allocatesExistingOrder() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);
        Timestamp originalUpdatedAt = jdbcTemplate.queryForObject(
                "select updated_at from orders where id = ?",
                Timestamp.class,
                orderId
        );

        Thread.sleep(20);

        mockMvc.perform(post("/api/v1/orders/{id}/allocate", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.status").value("ALLOCATED"));

        Map<String, Object> persistedOrder = jdbcTemplate.queryForMap(
                "select status, updated_at from orders where id = ?",
                orderId
        );

        assertEquals("ALLOCATED", persistedOrder.get("status"));
        Timestamp updatedAtAfterAllocation = (Timestamp) persistedOrder.get("updated_at");
        assertTrue(updatedAtAfterAllocation.toInstant().isAfter(originalUpdatedAt.toInstant()));
    }

    @Test
    void returnsNotFoundWhenAllocatingMissingOrder() throws Exception {
        UUID missingOrderId = UUID.randomUUID();

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/allocate", missingOrderId)),
                404,
                "ORDER_NOT_FOUND",
                "Order was not found",
                "/api/v1/orders/%s/allocate".formatted(missingOrderId)
        );
    }

    @Test
    void returnsBadRequestWhenAllocatingWithInvalidOrderId() throws Exception {
        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/allocate", "not-a-uuid")),
                400,
                "INVALID_ORDER_ID",
                "Order id must be a valid UUID",
                "/api/v1/orders/not-a-uuid/allocate"
        );
    }

    @Test
    void returnsConflictWhenAllocationIsNotAllowed() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        insertOrder(orderId, sellerId, "CANCELLED");

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/allocate", orderId)),
                409,
                "INVALID_ORDER_STATUS_TRANSITION",
                "Cannot transition order status from CANCELLED to ALLOCATED",
                "/api/v1/orders/%s/allocate".formatted(orderId)
        );

        String persistedStatus = jdbcTemplate.queryForObject(
                "select status from orders where id = ?",
                String.class,
                orderId
        );
        assertEquals("CANCELLED", persistedStatus);
    }

    @Test
    void marksAllocatedOrderReadyToShip() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);
        mockMvc.perform(post("/api/v1/orders/{id}/allocate", orderId))
                .andExpect(status().isOk());
        Timestamp originalUpdatedAt = jdbcTemplate.queryForObject(
                "select updated_at from orders where id = ?",
                Timestamp.class,
                orderId
        );

        Thread.sleep(20);

        mockMvc.perform(post("/api/v1/orders/{id}/ready-to-ship", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.status").value("READY_TO_SHIP"));

        Map<String, Object> persistedOrder = jdbcTemplate.queryForMap(
                "select status, updated_at from orders where id = ?",
                orderId
        );

        assertEquals("READY_TO_SHIP", persistedOrder.get("status"));
        Timestamp updatedAtAfterMarkingReadyToShip = (Timestamp) persistedOrder.get("updated_at");
        assertTrue(updatedAtAfterMarkingReadyToShip.toInstant().isAfter(originalUpdatedAt.toInstant()));
    }

    @Test
    void returnsNotFoundWhenMarkingReadyToShipMissingOrder() throws Exception {
        UUID missingOrderId = UUID.randomUUID();

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/ready-to-ship", missingOrderId)),
                404,
                "ORDER_NOT_FOUND",
                "Order was not found",
                "/api/v1/orders/%s/ready-to-ship".formatted(missingOrderId)
        );
    }

    @Test
    void returnsBadRequestWhenMarkingReadyToShipWithInvalidOrderId() throws Exception {
        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/ready-to-ship", "not-a-uuid")),
                400,
                "INVALID_ORDER_ID",
                "Order id must be a valid UUID",
                "/api/v1/orders/not-a-uuid/ready-to-ship"
        );
    }

    @Test
    void returnsConflictWhenMarkingReadyToShipIsNotAllowed() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        insertOrder(orderId, sellerId, "CREATED");

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/ready-to-ship", orderId)),
                409,
                "INVALID_ORDER_STATUS_TRANSITION",
                "Cannot transition order status from CREATED to READY_TO_SHIP",
                "/api/v1/orders/%s/ready-to-ship".formatted(orderId)
        );

        String persistedStatus = jdbcTemplate.queryForObject(
                "select status from orders where id = ?",
                String.class,
                orderId
        );
        assertEquals("CREATED", persistedStatus);
    }

    @Test
    void dispatchesReadyToShipOrder() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);
        mockMvc.perform(post("/api/v1/orders/{id}/allocate", orderId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/{id}/ready-to-ship", orderId))
                .andExpect(status().isOk());
        Timestamp originalUpdatedAt = jdbcTemplate.queryForObject(
                "select updated_at from orders where id = ?",
                Timestamp.class,
                orderId
        );

        Thread.sleep(20);

        mockMvc.perform(post("/api/v1/orders/{id}/dispatch", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.status").value("DISPATCHED"));

        Map<String, Object> persistedOrder = jdbcTemplate.queryForMap(
                "select status, updated_at from orders where id = ?",
                orderId
        );

        assertEquals("DISPATCHED", persistedOrder.get("status"));
        Timestamp updatedAtAfterDispatch = (Timestamp) persistedOrder.get("updated_at");
        assertTrue(updatedAtAfterDispatch.toInstant().isAfter(originalUpdatedAt.toInstant()));
    }

    @Test
    void returnsNotFoundWhenDispatchingMissingOrder() throws Exception {
        UUID missingOrderId = UUID.randomUUID();

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/dispatch", missingOrderId)),
                404,
                "ORDER_NOT_FOUND",
                "Order was not found",
                "/api/v1/orders/%s/dispatch".formatted(missingOrderId)
        );
    }

    @Test
    void returnsBadRequestWhenDispatchingWithInvalidOrderId() throws Exception {
        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/dispatch", "not-a-uuid")),
                400,
                "INVALID_ORDER_ID",
                "Order id must be a valid UUID",
                "/api/v1/orders/not-a-uuid/dispatch"
        );
    }

    @Test
    void returnsConflictWhenDispatchIsNotAllowed() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        insertOrder(orderId, sellerId, "CREATED");

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/dispatch", orderId)),
                409,
                "INVALID_ORDER_STATUS_TRANSITION",
                "Cannot transition order status from CREATED to DISPATCHED",
                "/api/v1/orders/%s/dispatch".formatted(orderId)
        );

        String persistedStatus = jdbcTemplate.queryForObject(
                "select status from orders where id = ?",
                String.class,
                orderId
        );
        assertEquals("CREATED", persistedStatus);
    }

    @Test
    void deliversDispatchedOrder() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);
        mockMvc.perform(post("/api/v1/orders/{id}/allocate", orderId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/{id}/ready-to-ship", orderId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/{id}/dispatch", orderId))
                .andExpect(status().isOk());
        Timestamp originalUpdatedAt = jdbcTemplate.queryForObject(
                "select updated_at from orders where id = ?",
                Timestamp.class,
                orderId
        );

        Thread.sleep(20);

        mockMvc.perform(post("/api/v1/orders/{id}/deliver", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        Map<String, Object> persistedOrder = jdbcTemplate.queryForMap(
                "select status, updated_at from orders where id = ?",
                orderId
        );

        assertEquals("DELIVERED", persistedOrder.get("status"));
        Timestamp updatedAtAfterDelivery = (Timestamp) persistedOrder.get("updated_at");
        assertTrue(updatedAtAfterDelivery.toInstant().isAfter(originalUpdatedAt.toInstant()));
    }

    @Test
    void returnsNotFoundWhenDeliveringMissingOrder() throws Exception {
        UUID missingOrderId = UUID.randomUUID();

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/deliver", missingOrderId)),
                404,
                "ORDER_NOT_FOUND",
                "Order was not found",
                "/api/v1/orders/%s/deliver".formatted(missingOrderId)
        );
    }

    @Test
    void returnsBadRequestWhenDeliveringWithInvalidOrderId() throws Exception {
        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/deliver", "not-a-uuid")),
                400,
                "INVALID_ORDER_ID",
                "Order id must be a valid UUID",
                "/api/v1/orders/not-a-uuid/deliver"
        );
    }

    @Test
    void returnsConflictWhenDeliveryIsNotAllowed() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        insertOrder(orderId, sellerId, "CREATED");

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/deliver", orderId)),
                409,
                "INVALID_ORDER_STATUS_TRANSITION",
                "Cannot transition order status from CREATED to DELIVERED",
                "/api/v1/orders/%s/deliver".formatted(orderId)
        );

        String persistedStatus = jdbcTemplate.queryForObject(
                "select status from orders where id = ?",
                String.class,
                orderId
        );
        assertEquals("CREATED", persistedStatus);
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

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/cancel", missingOrderId)),
                404,
                "ORDER_NOT_FOUND",
                "Order was not found",
                "/api/v1/orders/%s/cancel".formatted(missingOrderId)
        );
    }

    @Test
    void returnsBadRequestWhenCancellingWithInvalidOrderId() throws Exception {
        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/cancel", "not-a-uuid")),
                400,
                "INVALID_ORDER_ID",
                "Order id must be a valid UUID",
                "/api/v1/orders/not-a-uuid/cancel"
        );
    }

    @Test
    void returnsConflictWhenCancellationIsNotAllowed() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        insertOrder(orderId, sellerId, "DELIVERED");

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)),
                409,
                "INVALID_ORDER_STATUS_TRANSITION",
                "Cannot transition order status from DELIVERED to CANCELLED",
                "/api/v1/orders/%s/cancel".formatted(orderId)
        );

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

    private void assertOrderApiError(
            ResultActions resultActions,
            int httpStatus,
            String code,
            String message,
            String path
    ) throws Exception {
        resultActions
                .andExpect(status().is(httpStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(httpStatus))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private void insertOrder(UUID orderId, UUID sellerId, String status) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                "insert into orders (id, seller_id, status, created_at, updated_at) values (?, ?, ?, ?, ?)",
                orderId,
                sellerId,
                status,
                Timestamp.from(now),
                Timestamp.from(now)
        );
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
