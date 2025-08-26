-- User 테이블 데이터
-- UUID를 16진수 문자열로 변환해서 BINARY(16)에 저장
INSERT INTO User (UUID, email, password, nickname, profile_image, created_at) VALUES
(UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')), 'test@example.com', 'password123', '테스트유저1', null, CURRENT_TIMESTAMP),
(UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440001', '-', '')), 'student@example.com', 'password123', '학생1', null, CURRENT_TIMESTAMP),
(UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440002', '-', '')), 'teacher@example.com', 'password123', '선생님1', null, CURRENT_TIMESTAMP);

-- Subject 테이블 데이터
INSERT INTO subject (UUID, title, createdAt, user_uuid) VALUES
(UNHEX(REPLACE('660e8400-e29b-41d4-a716-446655440000', '-', '')), '고등학교 수학', CURRENT_TIMESTAMP, UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))),
(UNHEX(REPLACE('660e8400-e29b-41d4-a716-446655440001', '-', '')), '중학교 영어', CURRENT_TIMESTAMP, UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))),
(UNHEX(REPLACE('660e8400-e29b-41d4-a716-446655440002', '-', '')), '물리학 기초', CURRENT_TIMESTAMP, UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440001', '-', ''))),
(UNHEX(REPLACE('660e8400-e29b-41d4-a716-446655440003', '-', '')), '화학 실험', CURRENT_TIMESTAMP, UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440001', '-', ''))),
(UNHEX(REPLACE('660e8400-e29b-41d4-a716-446655440004', '-', '')), '한국사', CURRENT_TIMESTAMP, UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440002', '-', '')));