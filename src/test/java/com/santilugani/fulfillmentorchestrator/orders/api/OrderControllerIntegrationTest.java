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
import static org.junit.jupiter.api.Assertions.assertNull;
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
        jdbcTemplate.update("delete from fulfillment_nodes");
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

        assertTrue(result.getResponse().getContentAsString().contains("\"assignedFulfillmentNodeId\""));

        UUID orderId = extractOrderId(result.getResponse().getContentAsString());

        Map<String, Object> persistedOrder = jdbcTemplate.queryForMap(
                "select id, seller_id, status, fulfillment_node_id, allocated_at, created_at, updated_at from orders where id = ?",
                orderId
        );

        assertEquals(orderId, persistedOrder.get("id"));
        assertEquals(sellerId, persistedOrder.get("seller_id"));
        assertEquals("CREATED", persistedOrder.get("status"));
        assertNull(persistedOrder.get("fulfillment_node_id"));
        assertNull(persistedOrder.get("allocated_at"));
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

        assertOrderResponse(
                mockMvc.perform(get("/api/v1/orders/{id}", orderId)),
                orderId,
                sellerId,
                "CREATED",
                null
        );
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
    void allocatesExistingOrderToFulfillmentNodeAndReturnsItFromGetOrder() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);
        UUID fulfillmentNodeId = createActiveFulfillmentNode();
        Timestamp originalUpdatedAt = updatedAt(orderId);

        Thread.sleep(20);

        assertOrderResponse(
                allocateOrderRequest(orderId, fulfillmentNodeId),
                orderId,
                sellerId,
                "ALLOCATED",
                fulfillmentNodeId
        );

        assertOrderResponse(
                mockMvc.perform(get("/api/v1/orders/{id}", orderId)),
                orderId,
                sellerId,
                "ALLOCATED",
                fulfillmentNodeId
        );

        assertPersistedStatusAndUpdatedAt(orderId, "ALLOCATED", originalUpdatedAt);
        assertEquals(fulfillmentNodeId, persistedFulfillmentNodeId(orderId));
        assertNotNull(persistedAllocatedAt(orderId));
    }

    @Test
    void returnsNotFoundWhenAllocatingMissingOrder() throws Exception {
        UUID missingOrderId = UUID.randomUUID();
        UUID fulfillmentNodeId = createActiveFulfillmentNode();

        assertOrderApiError(
                allocateOrderRequest(missingOrderId, fulfillmentNodeId),
                404,
                "ORDER_NOT_FOUND",
                "Order was not found",
                "/api/v1/orders/%s/allocate".formatted(missingOrderId)
        );
    }

    @Test
    void returnsBadRequestWhenAllocatingWithInvalidOrderId() throws Exception {
        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/allocate", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fulfillmentNodeId\":\"%s\"}".formatted(UUID.randomUUID()))),
                400,
                "INVALID_ORDER_ID",
                "Order id must be a valid UUID",
                "/api/v1/orders/not-a-uuid/allocate"
        );
    }

    @Test
    void returnsBadRequestWhenAllocatingWithoutFulfillmentNodeId() throws Exception {
        UUID orderId = createOrder(UUID.randomUUID());

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/allocate", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")),
                400,
                "MISSING_FULFILLMENT_NODE_ID",
                "fulfillmentNodeId is required",
                "/api/v1/orders/%s/allocate".formatted(orderId)
        );

        assertEquals("CREATED", persistedStatus(orderId));
        assertNull(persistedFulfillmentNodeId(orderId));
    }

    @Test
    void returnsBadRequestWhenAllocatingWithInvalidFulfillmentNodeId() throws Exception {
        UUID orderId = createOrder(UUID.randomUUID());

        assertOrderApiError(
                mockMvc.perform(post("/api/v1/orders/{id}/allocate", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fulfillmentNodeId\":\"not-a-uuid\"}")),
                400,
                "INVALID_FULFILLMENT_NODE_ID",
                "Fulfillment node id must be a valid UUID",
                "/api/v1/orders/%s/allocate".formatted(orderId)
        );

        assertEquals("CREATED", persistedStatus(orderId));
        assertNull(persistedFulfillmentNodeId(orderId));
    }

    @Test
    void returnsNotFoundWhenAllocatingWithMissingFulfillmentNode() throws Exception {
        UUID orderId = createOrder(UUID.randomUUID());
        UUID missingFulfillmentNodeId = UUID.randomUUID();

        assertOrderApiError(
                allocateOrderRequest(orderId, missingFulfillmentNodeId),
                404,
                "FULFILLMENT_NODE_NOT_FOUND",
                "Fulfillment node was not found",
                "/api/v1/orders/%s/allocate".formatted(orderId)
        );

        assertEquals("CREATED", persistedStatus(orderId));
        assertNull(persistedFulfillmentNodeId(orderId));
    }

    @Test
    void returnsConflictWhenAllocatingWithInactiveFulfillmentNode() throws Exception {
        UUID orderId = createOrder(UUID.randomUUID());
        UUID fulfillmentNodeId = UUID.randomUUID();
        insertFulfillmentNode(fulfillmentNodeId, "AR-BUE-01", "Buenos Aires Node 1", 100, false);

        assertOrderApiError(
                allocateOrderRequest(orderId, fulfillmentNodeId),
                409,
                "FULFILLMENT_NODE_INACTIVE",
                "Fulfillment node is inactive",
                "/api/v1/orders/%s/allocate".formatted(orderId)
        );

        assertEquals("CREATED", persistedStatus(orderId));
        assertNull(persistedFulfillmentNodeId(orderId));
    }

    @Test
    void returnsConflictWhenAllocationIsNotAllowed() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID fulfillmentNodeId = createActiveFulfillmentNode();
        insertOrder(orderId, sellerId, "CANCELLED");

        assertOrderApiError(
                allocateOrderRequest(orderId, fulfillmentNodeId),
                409,
                "INVALID_ORDER_STATUS_TRANSITION",
                "Cannot transition order status from CANCELLED to ALLOCATED",
                "/api/v1/orders/%s/allocate".formatted(orderId)
        );

        assertPersistedStatus(orderId, "CANCELLED");
    }

    @Test
    void returnsConflictWhenFulfillmentNodeDailyCapacityIsReached() throws Exception {
        UUID fulfillmentNodeId = createActiveFulfillmentNode(1);
        UUID firstSellerId = UUID.randomUUID();
        UUID firstOrderId = createOrder(firstSellerId);
        UUID secondOrderId = createOrder(UUID.randomUUID());

        assertOrderResponse(
                allocateOrderRequest(firstOrderId, fulfillmentNodeId),
                firstOrderId,
                firstSellerId,
                "ALLOCATED",
                fulfillmentNodeId
        );

        assertOrderApiError(
                allocateOrderRequest(secondOrderId, fulfillmentNodeId),
                409,
                "FULFILLMENT_NODE_CAPACITY_EXCEEDED",
                "Fulfillment node daily capacity has been reached",
                "/api/v1/orders/%s/allocate".formatted(secondOrderId)
        );

        assertEquals(fulfillmentNodeId, persistedFulfillmentNodeId(firstOrderId));
        assertNotNull(persistedAllocatedAt(firstOrderId));
        assertEquals("CREATED", persistedStatus(secondOrderId));
        assertNull(persistedFulfillmentNodeId(secondOrderId));
        assertNull(persistedAllocatedAt(secondOrderId));
    }

    @Test
    void marksAllocatedOrderReadyToShip() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);
        UUID fulfillmentNodeId = allocateOrder(orderId);
        Timestamp originalUpdatedAt = updatedAt(orderId);

        Thread.sleep(20);

        assertOrderResponse(
                mockMvc.perform(post("/api/v1/orders/{id}/ready-to-ship", orderId)),
                orderId,
                sellerId,
                "READY_TO_SHIP",
                fulfillmentNodeId
        );

        assertPersistedStatusAndUpdatedAt(orderId, "READY_TO_SHIP", originalUpdatedAt);
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

        assertPersistedStatus(orderId, "CREATED");
    }

    @Test
    void dispatchesReadyToShipOrder() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);
        UUID fulfillmentNodeId = allocateOrder(orderId);
        markOrderReadyToShip(orderId);
        Timestamp originalUpdatedAt = updatedAt(orderId);

        Thread.sleep(20);

        assertOrderResponse(
                mockMvc.perform(post("/api/v1/orders/{id}/dispatch", orderId)),
                orderId,
                sellerId,
                "DISPATCHED",
                fulfillmentNodeId
        );

        assertPersistedStatusAndUpdatedAt(orderId, "DISPATCHED", originalUpdatedAt);
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

        assertPersistedStatus(orderId, "CREATED");
    }

    @Test
    void deliversDispatchedOrder() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);
        UUID fulfillmentNodeId = allocateOrder(orderId);
        markOrderReadyToShip(orderId);
        dispatchOrder(orderId);
        Timestamp originalUpdatedAt = updatedAt(orderId);

        Thread.sleep(20);

        assertOrderResponse(
                mockMvc.perform(post("/api/v1/orders/{id}/deliver", orderId)),
                orderId,
                sellerId,
                "DELIVERED",
                fulfillmentNodeId
        );

        assertPersistedStatusAndUpdatedAt(orderId, "DELIVERED", originalUpdatedAt);
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

        assertPersistedStatus(orderId, "CREATED");
    }

    @Test
    void cancelsExistingOrder() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID orderId = createOrder(sellerId);
        Timestamp originalUpdatedAt = updatedAt(orderId);

        Thread.sleep(20);

        assertOrderResponse(
                mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)),
                orderId,
                sellerId,
                "CANCELLED",
                null
        );

        assertPersistedStatusAndUpdatedAt(orderId, "CANCELLED", originalUpdatedAt);
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

        assertPersistedStatus(orderId, "DELIVERED");
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

    private void assertOrderResponse(
            ResultActions resultActions,
            UUID orderId,
            UUID sellerId,
            String status,
            UUID assignedFulfillmentNodeId
    ) throws Exception {
        ResultActions assertions = resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.status").value(status));

        if (assignedFulfillmentNodeId != null) {
            assertions.andExpect(jsonPath("$.assignedFulfillmentNodeId").value(assignedFulfillmentNodeId.toString()));
        }
    }

    private void assertPersistedStatus(UUID orderId, String expectedStatus) {
        assertEquals(expectedStatus, persistedStatus(orderId));
    }

    private void assertPersistedStatusAndUpdatedAt(UUID orderId, String expectedStatus, Timestamp originalUpdatedAt) {
        assertPersistedStatus(orderId, expectedStatus);
        assertTrue(updatedAt(orderId).toInstant().isAfter(originalUpdatedAt.toInstant()));
    }

    private UUID persistedFulfillmentNodeId(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "select fulfillment_node_id from orders where id = ?",
                UUID.class,
                orderId
        );
    }

    private Timestamp persistedAllocatedAt(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "select allocated_at from orders where id = ?",
                Timestamp.class,
                orderId
        );
    }

    private String persistedStatus(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "select status from orders where id = ?",
                String.class,
                orderId
        );
    }

    private Timestamp updatedAt(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "select updated_at from orders where id = ?",
                Timestamp.class,
                orderId
        );
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

    private void insertFulfillmentNode(
            UUID fulfillmentNodeId,
            String code,
            String name,
            int maxDailyCapacity,
            boolean active
    ) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                        insert into fulfillment_nodes
                        (id, code, name, max_daily_capacity, active, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                fulfillmentNodeId,
                code,
                name,
                maxDailyCapacity,
                active,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private UUID createActiveFulfillmentNode() {
        return createActiveFulfillmentNode(100);
    }

    private UUID createActiveFulfillmentNode(int maxDailyCapacity) {
        UUID fulfillmentNodeId = UUID.randomUUID();
        insertFulfillmentNode(
                fulfillmentNodeId,
                "AR-BUE-%s".formatted(fulfillmentNodeId.toString().substring(0, 4)),
                "Buenos Aires Node 1",
                maxDailyCapacity,
                true
        );
        return fulfillmentNodeId;
    }

    private ResultActions allocateOrderRequest(UUID orderId, UUID fulfillmentNodeId) throws Exception {
        return mockMvc.perform(post("/api/v1/orders/{id}/allocate", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fulfillmentNodeId\":\"%s\"}".formatted(fulfillmentNodeId)));
    }

    private UUID allocateOrder(UUID orderId) throws Exception {
        UUID fulfillmentNodeId = createActiveFulfillmentNode();
        allocateOrderRequest(orderId, fulfillmentNodeId)
                .andExpect(status().isOk());
        return fulfillmentNodeId;
    }

    private void markOrderReadyToShip(UUID orderId) throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/ready-to-ship", orderId))
                .andExpect(status().isOk());
    }

    private void dispatchOrder(UUID orderId) throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/dispatch", orderId))
                .andExpect(status().isOk());
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
