package com.rewardpoint.service.pointaccount.controller;

import com.rewardpoint.service.pointaccount.controller.dto.CreatePointAccountRequest;
import com.rewardpoint.service.pointaccount.controller.dto.PointAccountResponse;
import com.rewardpoint.service.pointaccount.service.PointAccountService;
import com.rewardpoint.service.pointaccount.service.dto.CreatePointAccountCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/point-accounts")
@RequiredArgsConstructor
public class PointAccountController {

    private final PointAccountService pointAccountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PointAccountResponse create(@Valid @RequestBody CreatePointAccountRequest request) {
        return PointAccountResponse.from(
                pointAccountService.create(new CreatePointAccountCommand(request.userId()))
        );
    }

    @GetMapping("/{userId}")
    public PointAccountResponse getByUserId(@PathVariable String userId) {
        return PointAccountResponse.from(pointAccountService.getByUserId(userId));
    }
}
