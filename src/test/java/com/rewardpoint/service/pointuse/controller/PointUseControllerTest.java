package com.rewardpoint.service.pointuse.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rewardpoint.service.pointearn.entity.PointGrant;
import com.rewardpoint.service.pointcore.entity.PointTransaction;
import com.rewardpoint.service.pointuse.entity.PointUse;
import com.rewardpoint.service.pointuse.entity.PointUseAllocation;
import com.rewardpoint.service.pointearn.repository.PointGrantRepository;
import com.rewardpoint.service.pointcore.repository.PointTransactionRepository;
import com.rewardpoint.service.pointuse.repository.PointUseAllocationRepository;
import com.rewardpoint.service.pointuse.repository.PointUseRepository;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.support.RestDocsUtils;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@DisplayName("포인트 사용 컨트롤러 테스트")
class PointUseControllerTest {

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

    @Autowired
    private PointUseRepository pointUseRepository;

    @Autowired
    private PointUseAllocationRepository pointUseAllocationRepository;

    @Test
    @DisplayName("수기 지급 포인트가 일반 포인트보다 우선 사용된다")
    void usesManualPointFirst() throws Exception {
        PointAccount account = pointAccountRepository.save(new PointAccount("manual-first-user"));

        JsonNode normalEarn = earn(account.getAccountId(), 1000, false, 30);
        JsonNode manualEarn = earn(account.getAccountId(), 500, true, 100);

        MvcResult useResult = mockMvc.perform(post("/api/points/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"orderNo":"ORDER-MANUAL","amount":1200,"description":"use"}
                                """.formatted(account.getAccountId())))
                .andDo(RestDocsUtils.documentWithPrettyPrint(
                        "point-use",
                        requestFields(
                                fieldWithPath("accountId").description("포인트 계정 ID"),
                                fieldWithPath("orderNo").description("주문 번호"),
                                fieldWithPath("amount").description("사용 금액"),
                                fieldWithPath("description").optional().description("사용 설명")
                        ),
                        RestDocsUtils.pointOperationResponseSnippet()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("USE"))
                .andExpect(jsonPath("$.currentBalance").value(300))
                .andReturn();

        JsonNode response = objectMapper.readTree(useResult.getResponse().getContentAsString());
        assertThat(response.get("lines").size()).isEqualTo(2);
        assertThat(response.get("lines").get(0).get("sourceTransactionKey").asText()).isEqualTo(manualEarn.get("transactionKey").asText());
        assertThat(response.get("lines").get(0).get("amount").asLong()).isEqualTo(500L);
        assertThat(response.get("lines").get(1).get("sourceTransactionKey").asText()).isEqualTo(normalEarn.get("transactionKey").asText());
        assertThat(response.get("lines").get(1).get("amount").asLong()).isEqualTo(700L);
    }

    @Test
    @DisplayName("사용취소 시 만료된 지급 건은 신규 적립으로 재발행된다")
    void reissuesExpiredGrantOnUseCancel() throws Exception {
        PointAccount account = pointAccountRepository.save(new PointAccount("cancel-user"));

        JsonNode earnA = earn(account.getAccountId(), 1000, false, 30);
        JsonNode earnB = earn(account.getAccountId(), 500, false, 60);

        MvcResult useResult = mockMvc.perform(post("/api/points/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"orderNo":"A1234","amount":1200,"description":"use"}
                                """.formatted(account.getAccountId())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode useResponse = objectMapper.readTree(useResult.getResponse().getContentAsString());
        String useTransactionKey = useResponse.get("transactionKey").asText();

        PointTransaction earnATransaction = pointTransactionRepository.findByTransactionKey(earnA.get("transactionKey").asText()).orElseThrow();
        PointGrant earnAGrant = pointGrantRepository.findByTransaction(earnATransaction).orElseThrow();
        setExpireAt(earnAGrant, LocalDateTime.now().minusDays(1));
        pointGrantRepository.saveAndFlush(earnAGrant);

        MvcResult cancelResult = mockMvc.perform(post("/api/points/use-cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"targetTransactionKey":"%s","cancelAmount":1100,"description":"cancel-use"}
                                """.formatted(account.getAccountId(), useTransactionKey)))
                .andDo(RestDocsUtils.documentWithPrettyPrint(
                        "point-use-cancel",
                        requestFields(
                                fieldWithPath("accountId").description("포인트 계정 ID"),
                                fieldWithPath("targetTransactionKey").description("취소할 사용 거래 키"),
                                fieldWithPath("cancelAmount").description("취소 금액"),
                                fieldWithPath("description").optional().description("사용취소 설명")
                        ),
                        RestDocsUtils.pointOperationResponseSnippet()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(1400))
                .andReturn();

        JsonNode cancelResponse = objectMapper.readTree(cancelResult.getResponse().getContentAsString());
        assertThat(cancelResponse.get("lines").size()).isEqualTo(2);

        JsonNode firstLine = cancelResponse.get("lines").get(0);
        JsonNode secondLine = cancelResponse.get("lines").get(1);

        assertThat(firstLine.get("sourceTransactionKey").asText()).isEqualTo(earnA.get("transactionKey").asText());
        assertThat(firstLine.get("amount").asLong()).isEqualTo(1000L);
        assertThat(firstLine.get("restoreType").asText()).isEqualTo("REISSUE_NEW_LOT");
        assertThat(firstLine.get("reissuedTransactionKey").asText()).isNotBlank();

        assertThat(secondLine.get("sourceTransactionKey").asText()).isEqualTo(earnB.get("transactionKey").asText());
        assertThat(secondLine.get("amount").asLong()).isEqualTo(100L);
        assertThat(secondLine.get("restoreType").asText()).isEqualTo("RESTORE_ORIGINAL_LOT");
    }

    @Test
    @DisplayName("포인트 사용 내역은 주문번호와 allocation으로 추적할 수 있다")
    @Transactional
    void tracksOrderNoAndAllocationsOnUse() throws Exception {
        PointAccount account = pointAccountRepository.save(new PointAccount("trace-user"));

        JsonNode earnA = earn(account.getAccountId(), 1000, false, 30);
        JsonNode earnB = earn(account.getAccountId(), 500, false, 60);

        MvcResult useResult = mockMvc.perform(post("/api/points/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":%d,"orderNo":"ORDER-TRACE","amount":1200,"description":"use"}
                                """.formatted(account.getAccountId())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode useResponse = objectMapper.readTree(useResult.getResponse().getContentAsString());
        PointTransaction useTransaction = pointTransactionRepository.findByTransactionKey(
                useResponse.get("transactionKey").asText()
        ).orElseThrow();
        PointUse pointUse = pointUseRepository.findByTransaction(useTransaction).orElseThrow();

        List<PointUseAllocation> allocations = pointUseAllocationRepository.findByPointUseOrderByAllocationIdAsc(pointUse);

        assertThat(pointUse.getOrderNo()).isEqualTo("ORDER-TRACE");
        assertThat(allocations).hasSize(2);
        assertThat(allocations.get(0).getGrant().getTransaction().getTransactionKey()).isEqualTo(earnA.get("transactionKey").asText());
        assertThat(allocations.get(0).getAllocatedAmount()).isEqualTo(1000L);
        assertThat(allocations.get(1).getGrant().getTransaction().getTransactionKey()).isEqualTo(earnB.get("transactionKey").asText());
        assertThat(allocations.get(1).getAllocatedAmount()).isEqualTo(200L);
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

    private void setExpireAt(PointGrant grant, LocalDateTime expireAt) {
        try {
            Field field = PointGrant.class.getDeclaredField("expireAt");
            field.setAccessible(true);
            field.set(grant, expireAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
