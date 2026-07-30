package com.santilugani.fulfillmentorchestrator.fulfillment.api;

import com.santilugani.fulfillmentorchestrator.fulfillment.application.CreateFulfillmentNodeCommand;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.CreateFulfillmentNodeUseCase;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.FulfillmentNodeResult;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.GetFulfillmentNodeWorkingDaysQuery;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.GetFulfillmentNodeWorkingDaysUseCase;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.GetFulfillmentNodeByIdQuery;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.GetFulfillmentNodeByIdUseCase;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.ListFulfillmentNodesUseCase;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.ReplaceFulfillmentNodeWorkingDaysCommand;
import com.santilugani.fulfillmentorchestrator.fulfillment.application.ReplaceFulfillmentNodeWorkingDaysUseCase;
import com.santilugani.fulfillmentorchestrator.fulfillment.domain.FulfillmentNodeId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/fulfillment-nodes")
public class FulfillmentNodeController {

    private final CreateFulfillmentNodeUseCase createFulfillmentNodeUseCase;
    private final GetFulfillmentNodeByIdUseCase getFulfillmentNodeByIdUseCase;
    private final ListFulfillmentNodesUseCase listFulfillmentNodesUseCase;
    private final GetFulfillmentNodeWorkingDaysUseCase getFulfillmentNodeWorkingDaysUseCase;
    private final ReplaceFulfillmentNodeWorkingDaysUseCase replaceFulfillmentNodeWorkingDaysUseCase;

    public FulfillmentNodeController(
            CreateFulfillmentNodeUseCase createFulfillmentNodeUseCase,
            GetFulfillmentNodeByIdUseCase getFulfillmentNodeByIdUseCase,
            ListFulfillmentNodesUseCase listFulfillmentNodesUseCase,
            GetFulfillmentNodeWorkingDaysUseCase getFulfillmentNodeWorkingDaysUseCase,
            ReplaceFulfillmentNodeWorkingDaysUseCase replaceFulfillmentNodeWorkingDaysUseCase
    ) {
        this.createFulfillmentNodeUseCase = Objects.requireNonNull(
                createFulfillmentNodeUseCase,
                "createFulfillmentNodeUseCase must not be null"
        );
        this.getFulfillmentNodeByIdUseCase = Objects.requireNonNull(
                getFulfillmentNodeByIdUseCase,
                "getFulfillmentNodeByIdUseCase must not be null"
        );
        this.listFulfillmentNodesUseCase = Objects.requireNonNull(
                listFulfillmentNodesUseCase,
                "listFulfillmentNodesUseCase must not be null"
        );
        this.getFulfillmentNodeWorkingDaysUseCase = Objects.requireNonNull(
                getFulfillmentNodeWorkingDaysUseCase,
                "getFulfillmentNodeWorkingDaysUseCase must not be null"
        );
        this.replaceFulfillmentNodeWorkingDaysUseCase = Objects.requireNonNull(
                replaceFulfillmentNodeWorkingDaysUseCase,
                "replaceFulfillmentNodeWorkingDaysUseCase must not be null"
        );
    }

    @PostMapping
    public ResponseEntity<FulfillmentNodeResponse> createFulfillmentNode(
            @Valid @RequestBody CreateFulfillmentNodeRequest request
    ) {
        FulfillmentNodeResult result = createFulfillmentNodeUseCase.createFulfillmentNode(
                new CreateFulfillmentNodeCommand(request.code(), request.name(), request.maxDailyCapacity())
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FulfillmentNodeResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FulfillmentNodeResponse> getFulfillmentNodeById(@PathVariable String id) {
        FulfillmentNodeResult result = getFulfillmentNodeByIdUseCase.getFulfillmentNodeById(
                new GetFulfillmentNodeByIdQuery(parseFulfillmentNodeId(id))
        );

        return ResponseEntity.ok(FulfillmentNodeResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<List<FulfillmentNodeResponse>> listFulfillmentNodes() {
        List<FulfillmentNodeResponse> response = listFulfillmentNodesUseCase.listFulfillmentNodes().stream()
                .map(FulfillmentNodeResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/working-days")
    public ResponseEntity<FulfillmentNodeWorkingDaysResponse> getFulfillmentNodeWorkingDays(@PathVariable String id) {
        return ResponseEntity.ok(
                FulfillmentNodeWorkingDaysResponse.from(
                        getFulfillmentNodeWorkingDaysUseCase.getWorkingDays(
                                new GetFulfillmentNodeWorkingDaysQuery(parseFulfillmentNodeId(id))
                        )
                )
        );
    }

    @PutMapping("/{id}/working-days")
    public ResponseEntity<FulfillmentNodeWorkingDaysResponse> replaceFulfillmentNodeWorkingDays(
            @PathVariable String id,
            @RequestBody ReplaceFulfillmentNodeWorkingDaysRequest request
    ) {
        return ResponseEntity.ok(
                FulfillmentNodeWorkingDaysResponse.from(
                        replaceFulfillmentNodeWorkingDaysUseCase.replaceWorkingDays(
                                new ReplaceFulfillmentNodeWorkingDaysCommand(
                                        parseFulfillmentNodeId(id),
                                        parseWorkingDays(request)
                                )
                        )
                )
        );
    }

    private FulfillmentNodeId parseFulfillmentNodeId(String id) {
        try {
            return FulfillmentNodeId.from(id);
        } catch (IllegalArgumentException exception) {
            throw new InvalidFulfillmentNodeIdException(id);
        }
    }

    private Set<DayOfWeek> parseWorkingDays(ReplaceFulfillmentNodeWorkingDaysRequest request) {
        if (request == null || request.workingDays() == null || request.workingDays().isEmpty()) {
            throw new InvalidFulfillmentNodeWorkingDaysRequestException();
        }

        Set<DayOfWeek> workingDays = EnumSet.noneOf(DayOfWeek.class);
        for (String workingDay : request.workingDays()) {
            if (workingDay == null || workingDay.isBlank()) {
                throw new InvalidFulfillmentNodeWorkingDaysRequestException();
            }

            try {
                workingDays.add(DayOfWeek.valueOf(workingDay.trim()));
            } catch (IllegalArgumentException exception) {
                throw new InvalidFulfillmentNodeWorkingDaysRequestException();
            }
        }

        return workingDays;
    }
}
