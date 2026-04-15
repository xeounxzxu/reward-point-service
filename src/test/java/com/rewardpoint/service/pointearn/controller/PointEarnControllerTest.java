package com.rewardpoint.service.pointearn.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rewardpoint.service.pointledger.entity.PointGrant;
import com.rewardpoint.service.pointledger.entity.PointTransaction;
import com.rewardpoint.service.pointledger.repository.PointGrantRepository;
import com.rewardpoint.service.pointledger.repository.PointTransactionRepository;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("포인트 적립 컨트롤러 테스트")
class PointEarnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private PointGrantRepository pointGrantRepository;

    @Test
    @DisplayName("포인트 적립에 성공하면 transactionKey와 현재 잔액이 반환된다")
    void earnsPoint() throws Exception {
        PointAccount account = pointAccountRepository.save(new PointAccount("earn-user"));

        mockMvc.perform(post("/api/points/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"amount":1000,"manual":false,"expireDays":30,"description":"earn"}
                                """.formatted(account.getAccountId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionKey").isString())
                .andExpect(jsonPath("$.transactionType").value("EARN"))
                .andExpect(jsonPath("$.currentBalance").value(1000));
    }

    @Test
    @DisplayName("부분 사용된 적립 포인트는 적립취소할 수 없다")
    void cannotCancelUsedEarnPoint() throws Exception {
        PointAccount account = pointAccountRepository.save(new PointAccount("earn-cancel-user"));
        String earnTransactionKey = earn(account.getAccountId(), 1000, false, 30).get("transactionKey").asText();

        mockMvc.perform(post("/api/points/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"orderNo":"ORDER-1","amount":500,"description":"use"}
                                """.formatted(account.getAccountId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/points/earn-cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"targetTransactionKey":"%s","description":"cancel"}
                                """.formatted(account.getAccountId(), earnTransactionKey)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("이미 사용된 적립 포인트는 취소할 수 없습니다."));
    }

    private JsonNode earn(Long accountId, long amount, boolean manual, int expireDays) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/points/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"amount":%d,"manual":%s,"expireDays":%d,"description":"earn"}
                                """.formatted(accountId, amount, manual, expireDays)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
