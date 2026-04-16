package com.rewardpoint.service.pointearn.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.support.RestDocsUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@DisplayName("포인트 적립 컨트롤러 테스트")
class PointEarnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Test
    @DisplayName("포인트 적립에 성공하면 transactionKey와 현재 잔액이 반환된다")
    void earnsPoint() throws Exception {
        PointAccount account = pointAccountRepository.save(new PointAccount("earn-user"));

        mockMvc.perform(post("/api/points/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"amount":1000,"manual":false,"expireDays":30,"description":"earn"}
                                """.formatted(account.getAccountId())))
                .andDo(RestDocsUtils.documentWithPrettyPrint(
                        "point-earn",
                        requestFields(
                                fieldWithPath("accountId").description("포인트 계정 ID"),
                                fieldWithPath("amount").description("적립 금액"),
                                fieldWithPath("manual").description("관리자 수기 지급 여부"),
                                fieldWithPath("expireDays").optional().description("만료일 수. 비어 있으면 정책 기본값 사용"),
                                fieldWithPath("description").optional().description("적립 설명")
                        ),
                        RestDocsUtils.pointOperationResponseSnippet()
                ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionKey").isString())
                .andExpect(jsonPath("$.transactionType").value("EARN"))
                .andExpect(jsonPath("$.currentBalance").value(1000));
    }

    @Test
    @DisplayName("미사용 적립 포인트는 적립취소할 수 있다")
    void cancelsEarnPoint() throws Exception {
        PointAccount account = pointAccountRepository.save(new PointAccount("earn-cancel-success-user"));
        String earnTransactionKey = earn(account.getAccountId(), 1000, false, 30).get("transactionKey").asText();

        mockMvc.perform(post("/api/points/earn-cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"targetTransactionKey":"%s","description":"cancel"}
                                """.formatted(account.getAccountId(), earnTransactionKey)))
                .andDo(RestDocsUtils.documentWithPrettyPrint(
                        "point-earn-cancel",
                        requestFields(
                                fieldWithPath("accountId").description("포인트 계정 ID"),
                                fieldWithPath("targetTransactionKey").description("취소할 적립 거래 키"),
                                fieldWithPath("description").optional().description("적립취소 설명")
                        ),
                        RestDocsUtils.pointOperationResponseSnippet()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("EARN_CANCEL"))
                .andExpect(jsonPath("$.amount").value(1000))
                .andExpect(jsonPath("$.currentBalance").value(0));
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
