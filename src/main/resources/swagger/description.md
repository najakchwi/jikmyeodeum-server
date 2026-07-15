### LetSports API 명세서입니다.

---

### API 기본 정보
각 API 문서에서 HTTP Method, Endpoint, API 이름을 함께 확인할 수 있습니다.

---

### 공통 응답 형식
모든 API는 아래 `ApiResponse` 형식으로 응답합니다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `isSuccess` | Boolean | 요청 성공 여부 |
| `data` | T? | 응답 데이터 (실패 시 null) |
| `errorCode` | String? | 에러 코드 (성공 시 null) |
| `message` | String? | 에러 메시지 (성공 시 null) |
| `timestamp` | LocalDateTime | 응답 시각 |

**성공 응답 예시**
```json
{
  "isSuccess": true,
  "data": { ... },
  "errorCode": null,
  "message": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

**실패 응답 예시**
```json
{
  "isSuccess": false,
  "data": null,
  "errorCode": "G400",
  "message": "Invalid input",
  "timestamp": "2024-01-01T00:00:00"
}
```
