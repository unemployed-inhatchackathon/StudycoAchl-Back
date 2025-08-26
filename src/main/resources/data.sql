INSERT INTO users (uuid, email, password, nickname, created_at) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'test@example.com', 'password123', '테스트유저1', CURRENT_TIMESTAMP);

INSERT INTO subjects (uuid, title, createdAt, user_uuid) VALUES
('660e8400-e29b-41d4-a716-446655440000', '고등학교 수학', CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440000');