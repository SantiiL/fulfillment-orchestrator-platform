package com.santilugani.fulfillmentorchestrator.fulfillment.api;

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
class FulfillmentNodeControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcTemplate.update("delete from fulfillment_nodes");
    }

    @Test
    void createsFulfillmentNodeAndPersistsIt() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/fulfillment-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "AR-BUE-01",
                                  "name": "Buenos Aires Node 1",
                                  "maxDailyCapacity": 100
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("AR-BUE-01"))
                .andExpect(jsonPath("$.name").value("Buenos Aires Node 1"))
                .andExpect(jsonPath("$.maxDailyCapacity").value(100))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        UUID fulfillmentNodeId = extractFulfillmentNodeId(result.getResponse().getContentAsString());

        Map<String, Object> persistedFulfillmentNode = jdbcTemplate.queryForMap(
                "select id, code, name, max_daily_capacity, active, created_at, updated_at from fulfillment_nodes where id = ?",
                fulfillmentNodeId
        );

        assertEquals(fulfillmentNodeId, persistedFulfillmentNode.get("id"));
        assertEquals("AR-BUE-01", persistedFulfillmentNode.get("code"));
        assertEquals("Buenos Aires Node 1", persistedFulfillmentNode.get("name"));
        assertEquals(100, persistedFulfillmentNode.get("max_daily_capacity"));
        assertEquals(true, persistedFulfillmentNode.get("active"));
        assertNotNull(persistedFulfillmentNode.get("created_at"));
        assertNotNull(persistedFulfillmentNode.get("updated_at"));
    }

    @Test
    void returnsFulfillmentNodeById() throws Exception {
        UUID fulfillmentNodeId = UUID.randomUUID();
        insertFulfillmentNode(fulfillmentNodeId, "AR-BUE-01", "Buenos Aires Node 1", 100, true);

        mockMvc.perform(get("/api/v1/fulfillment-nodes/{id}", fulfillmentNodeId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(fulfillmentNodeId.toString()))
                .andExpect(jsonPath("$.code").value("AR-BUE-01"))
                .andExpect(jsonPath("$.name").value("Buenos Aires Node 1"))
                .andExpect(jsonPath("$.maxDailyCapacity").value(100))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void listsCreatedFulfillmentNodes() throws Exception {
        UUID firstFulfillmentNodeId = UUID.randomUUID();
        UUID secondFulfillmentNodeId = UUID.randomUUID();
        insertFulfillmentNode(firstFulfillmentNodeId, "AR-BUE-02", "Buenos Aires Node 2", 200, true);
        insertFulfillmentNode(secondFulfillmentNodeId, "AR-BUE-01", "Buenos Aires Node 1", 100, true);

        mockMvc.perform(get("/api/v1/fulfillment-nodes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(secondFulfillmentNodeId.toString()))
                .andExpect(jsonPath("$[0].code").value("AR-BUE-01"))
                .andExpect(jsonPath("$[1].id").value(firstFulfillmentNodeId.toString()))
                .andExpect(jsonPath("$[1].code").value("AR-BUE-02"));
    }

    @Test
    void returnsBadRequestWhenFulfillmentNodeIdIsNotAValidUuid() throws Exception {
        assertFulfillmentNodeApiError(
                mockMvc.perform(get("/api/v1/fulfillment-nodes/{id}", "not-a-uuid")),
                400,
                "INVALID_FULFILLMENT_NODE_ID",
                "Fulfillment node id must be a valid UUID",
                "/api/v1/fulfillment-nodes/not-a-uuid"
        );
    }

    @Test
    void returnsNotFoundWhenFulfillmentNodeDoesNotExist() throws Exception {
        UUID missingFulfillmentNodeId = UUID.randomUUID();

        assertFulfillmentNodeApiError(
                mockMvc.perform(get("/api/v1/fulfillment-nodes/{id}", missingFulfillmentNodeId)),
                404,
                "FULFILLMENT_NODE_NOT_FOUND",
                "Fulfillment node was not found",
                "/api/v1/fulfillment-nodes/%s".formatted(missingFulfillmentNodeId)
        );
    }

    @Test
    void returnsBadRequestWhenCreateRequestIsInvalid() throws Exception {
        assertFulfillmentNodeApiError(
                mockMvc.perform(post("/api/v1/fulfillment-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "",
                                  "name": "Buenos Aires Node 1",
                                  "maxDailyCapacity": 100
                                }
                                """)),
                400,
                "INVALID_FULFILLMENT_NODE_REQUEST",
                "code is required",
                "/api/v1/fulfillment-nodes"
        );

        assertEquals(0, fulfillmentNodeCount());
    }

    @Test
    void returnsConflictWhenCodeAlreadyExists() throws Exception {
        insertFulfillmentNode(UUID.randomUUID(), "AR-BUE-01", "Buenos Aires Node 1", 100, true);

        assertFulfillmentNodeApiError(
                mockMvc.perform(post("/api/v1/fulfillment-nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "AR-BUE-01",
                                  "name": "Buenos Aires Node 2",
                                  "maxDailyCapacity": 200
                                }
                                """)),
                409,
                "FULFILLMENT_NODE_CODE_ALREADY_EXISTS",
                "Fulfillment node code already exists",
                "/api/v1/fulfillment-nodes"
        );

        assertEquals(1, fulfillmentNodeCount());
    }

    private UUID extractFulfillmentNodeId(String responseBody) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(responseBody);
        assertTrue(matcher.find(), "response body should contain an id");
        return UUID.fromString(matcher.group(1));
    }

    private void assertFulfillmentNodeApiError(
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

    private int fulfillmentNodeCount() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from fulfillment_nodes", Integer.class);
        return count == null ? 0 : count;
    }
}
