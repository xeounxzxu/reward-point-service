package com.rewardpoint.service.pointuse.controller;

import com.rewardpoint.service.pointcore.support.dto.PointOperationResponse;
import com.rewardpoint.service.pointcore.support.dto.PointOperationResult;
import com.rewardpoint.service.pointuse.controller.dto.CancelUsePointRequest;
import com.rewardpoint.service.pointuse.controller.dto.UsePointRequest;
import com.rewardpoint.service.pointuse.service.PointUseService;
import com.rewardpoint.service.pointuse.service.dto.CancelUsePointCommand;
import com.rewardpoint.service.pointuse.service.dto.UsePointCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointUseController {

    private final PointUseService pointUseService;

    @PostMapping("/use")
    public PointOperationResponse use(@Valid @RequestBody UsePointRequest request) {
        UsePointCommand command = new UsePointCommand(
                request.accountId(),
                request.orderNo(),
                request.amount(),
                request.description()
        );

        PointOperationResult pointOperationResult = pointUseService.use(command);

        return PointOperationResponse.from(pointOperationResult);
    }

    @PostMapping("/use-cancel")
    public PointOperationResponse cancelUse(@Valid @RequestBody CancelUsePointRequest request) {
        CancelUsePointCommand command = new CancelUsePointCommand(
                request.accountId(),
                request.targetTransactionKey(),
                request.cancelAmount(),
                request.description()
        );

        PointOperationResult pointOperationResult = pointUseService.cancelUse(command);

        return PointOperationResponse.from(pointOperationResult);
    }
}
