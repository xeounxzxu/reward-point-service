package com.rewardpoint.service.pointearn.controller;

import com.rewardpoint.service.pointearn.controller.dto.CancelEarnPointRequest;
import com.rewardpoint.service.pointearn.controller.dto.EarnPointRequest;
import com.rewardpoint.service.pointearn.service.PointEarnService;
import com.rewardpoint.service.pointearn.service.dto.CancelEarnPointCommand;
import com.rewardpoint.service.pointearn.service.dto.EarnPointCommand;
import com.rewardpoint.service.pointledger.support.dto.PointOperationResult;
import com.rewardpoint.service.pointledger.support.dto.PointOperationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointEarnController {

    private final PointEarnService pointEarnService;

    @PostMapping("/earn")
    @ResponseStatus(HttpStatus.CREATED)
    public PointOperationResponse earn(@Valid @RequestBody EarnPointRequest request) {
        EarnPointCommand command = new EarnPointCommand(
                request.accountId(),
                request.amount(),
                request.manual(),
                request.expireDays(),
                request.description()
        );

        PointOperationResult pointOperationResult = pointEarnService.earn(command);

        return PointOperationResponse.from(pointOperationResult);
    }

    @PostMapping("/earn-cancel")
    public PointOperationResponse cancelEarn(@Valid @RequestBody CancelEarnPointRequest request) {
        CancelEarnPointCommand command = new CancelEarnPointCommand(
                request.accountId(),
                request.targetTransactionKey(),
                request.description()
        );

        PointOperationResult pointOperationResult = pointEarnService.cancelEarn(command);

        return PointOperationResponse.from(pointOperationResult);
    }
}
