# Spring AI Day 1 - 주문 요약 API

교재의 Day 1 실습과 `SpringAI_Day1_P331_P343.pdf` 결과를 재현한 프로젝트다. 주문번호와 사용자 ID로 본인 주문을 확인한 뒤 OpenAI로 주문 상태를 한국어 한 문장으로 요약한다. AI 호출에 실패해도 상품명과 상태를 이용한 기본 요약을 반환한다.

## 실행

JDK 21과 OpenAI API 키가 필요하다.

```bash
export SPRING_AI_OPENAI_API_KEY="발급받은_API_키"
./gradlew bootRun
```

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- H2 Console: <http://localhost:8080/h2-console>
- H2 JDBC URL: `jdbc:h2:mem:day1` (사용자 `sa`, 비밀번호 없음)

## API 확인

```bash
# 정상 주문
curl -i 'http://localhost:8080/lab1/orders/12345/summary?userId=user1'

# 다른 사용자의 주문 - 404
curl -i 'http://localhost:8080/lab1/orders/99999/summary?userId=user1'

# 존재하지 않는 주문 - 404
curl -i 'http://localhost:8080/lab1/orders/00000/summary?userId=user1'

# 필수 파라미터 누락 - 400
curl -i 'http://localhost:8080/lab1/orders/12345/summary'
```

API 키, 통신, 호출 한도 등의 문제로 모델 호출이 실패하면 정상 주문 요청은 HTTP 200과 함께 `무선 이어폰 · 배송 중` 형태의 폴백 요약을 반환한다.

## 테스트

```bash
./gradlew test
```
