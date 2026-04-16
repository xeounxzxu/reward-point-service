package com.rewardpoint.service.pointaccount.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
@DisplayName("포인트 계정 컨트롤러 테스트")
class PointAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("포인트 계정을 생성하면 201과 생성된 계정 정보가 반환된다")
    void createsPointAccount() throws Exception {
        mockMvc.perform(post("/api/point-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-1"}
                                """))
                .andDo(RestDocsUtils.documentWithPrettyPrint(
                        "point-account-create",
                        requestFields(
                                fieldWithPath("userId").description("포인트 계정을 생성할 사용자 식별자")
                        ),
                        RestDocsUtils.pointAccountResponseSnippet()
                ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").isNumber())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentBalance").value(0));
    }

    @Test
    @DisplayName("accountId로 포인트 계정을 조회하면 200과 계정 정보가 반환된다")
    void getsPointAccountByAccountId() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/point-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-lookup"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String accountId = responseBody.replaceAll(".*\"accountId\":(\\d+).*", "$1");
        assertThat(accountId).isNotBlank();

        mockMvc.perform(get("/api/point-accounts/{accountId}", accountId))
                .andDo(RestDocsUtils.documentWithPrettyPrint(
                        "point-account-get",
                        pathParameters(
                                parameterWithName("accountId").description("조회할 포인트 계정 ID")
                        ),
                        RestDocsUtils.pointAccountResponseSnippet()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-lookup"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("중복된 userId로 포인트 계정을 생성하면 409가 반환된다")
    void returnsConflictWhenCreatingDuplicatePointAccount() throws Exception {
        mockMvc.perform(post("/api/point-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"duplicate-user"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/point-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"userId":"duplicate-user"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("포인트 계정이 이미 존재합니다. userId=duplicate-user"));
    }

    @Test
    @DisplayName("userId가 비어 있으면 400이 반환된다")
    void returnsBadRequestWhenUserIdIsBlank() throws Exception {
        mockMvc.perform(post("/api/point-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"userId":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("userId: userId는 필수입니다."));
    }

    @Test
    @DisplayName("존재하지 않는 accountId로 조회하면 404가 반환된다")
    void returnsNotFoundWhenPointAccountDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/point-accounts/{accountId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("포인트 계정을 찾을 수 없습니다. accountId=999999"));
    }
}
