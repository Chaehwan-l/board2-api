package lch.global.common;

// try-catch로 코드가 지저분해지는 것을 막고, 프론트와 통신할 때 항상 일정한 JSON 규격을 내려주기 위한 세팅
// Record를 사용하여 불변 DTO를 매우 간결하게 생성
// 프론트엔드(Vercel)와 통신할 때 항상 이 포맷으로 JSON이 직렬화됨
public record ApiResponse<T>(
    boolean success,
    String message,
    T data
) {
    // 성공 시 반환할 정적 팩토리 메서드
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    // 실패 시 반환할 정적 팩토리 메서드 (에러는 data가 없으므로 null 처리)
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}