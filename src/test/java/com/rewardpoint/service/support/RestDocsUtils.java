package com.rewardpoint.service.support;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.snippet.Snippet;

public final class RestDocsUtils {

    private RestDocsUtils() {
    }

    public static RestDocumentationResultHandler documentWithPrettyPrint(String identifier, Snippet... snippets) {
        return document(
                identifier,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                snippets
        );
    }

    public static FieldDescriptor[] pointAccountResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("accountId").description("포인트 계정 ID"),
                fieldWithPath("userId").description("사용자 식별자"),
                fieldWithPath("status").description("포인트 계정 상태"),
                fieldWithPath("currentBalance").description("현재 포인트 잔액"),
                fieldWithPath("createdAt").description("계정 생성 시각"),
                fieldWithPath("updatedAt").description("계정 수정 시각")
        };
    }

    public static FieldDescriptor[] pointOperationResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("accountId").description("포인트 계정 ID"),
                fieldWithPath("transactionKey").description("포인트 거래 키"),
                fieldWithPath("transactionType").description("포인트 거래 유형"),
                fieldWithPath("amount").description("이번 거래 금액"),
                fieldWithPath("currentBalance").description("거래 후 현재 포인트 잔액"),
                fieldWithPath("orderNo").optional().description("주문 번호. 적립 계열 거래에서는 비어 있을 수 있다"),
                fieldWithPath("lines").description("거래 상세 라인 목록"),
                fieldWithPath("lines[].sourceTransactionKey").description("원본 적립 또는 사용 거래 키"),
                fieldWithPath("lines[].amount").description("라인별 처리 금액"),
                fieldWithPath("lines[].restoreType").optional().description("복원 방식. 사용취소에서만 값이 존재한다"),
                fieldWithPath("lines[].reissuedTransactionKey").optional().description("재적립 거래 키. 만료 지급 건 재발행 시에만 값이 존재한다")
        };
    }

    public static Snippet pointAccountResponseSnippet() {
        return responseFields(pointAccountResponseFields());
    }

    public static Snippet pointOperationResponseSnippet() {
        return responseFields(pointOperationResponseFields());
    }
}
