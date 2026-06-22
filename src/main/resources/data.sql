-- INSERT IGNORE INTO profile_character (id, name, image_url, type, created_at, updated_at)
-- VALUES (1, '기본 캐릭터', '/images/default.png', 'DEFAULT', NOW(), NOW());
--
-- -- 사이트 관리자 테스트 계정 (ADMIN)
-- -- 평문 비밀번호: Admin1234!
-- INSERT IGNORE INTO account (nickname, email, password, phone, role, notification_enabled, age_visible, gender_visible, email_visible, created_at, updated_at)
-- VALUES ('grimgate-admin', 'admin@grimgate.com', '$2a$10$cgKJRbRZrArZtYVAzTwl2OEDbzmuSgyUxUCaQdXKPq2D9SlRapysu', '01000000000', 'ADMIN', true, true, true, true, NOW(), NOW());
INSERT IGNORE INTO title (id, name, description, min_success_rate, max_success_rate, required_clear_count, created_at, updated_at)
VALUES
(1, '쫄보',           '성공률 30% 미만',                    0,  29, null, NOW(), NOW()),
(2, '일반인',         '성공률 30~50%',                      30, 49, null, NOW(), NOW()),
(3, '강심장',         '성공률 50~70%',                      50, 69, null, NOW(), NOW()),
(4, '오컬트동호회장', '성공률 70~85%',                      70, 84, null, NOW(), NOW()),
(5, '퇴마사',         '성공률 85% 이상 + 5회 이상 클리어',  85, 100, 5,  NOW(), NOW());

INSERT IGNORE INTO achievement (id, name, description, condition_type, condition_value, created_at, updated_at)
VALUES
(1, '첫 발걸음',     '처음으로 방탈출을 완료했습니다.',      'TOTAL_PLAY_COUNT', 1,  NOW(), NOW()),
(2, '방탈출 병아리', '방탈출을 3회 완료했습니다.',           'TOTAL_PLAY_COUNT', 3,  NOW(), NOW()),
(3, '열쇠 수집가',   '방탈출을 7회 완료했습니다.',           'TOTAL_PLAY_COUNT', 7,  NOW(), NOW()),
(4, '단서 사냥꾼',   '방탈출을 15회 완료했습니다.',          'TOTAL_PLAY_COUNT', 15, NOW(), NOW()),
(5, '방탈출 지배자', '방탈출을 30회 완료했습니다.',          'TOTAL_PLAY_COUNT', 30, NOW(), NOW()),
(6, '스피드 러너',   '40분 이내로 방탈출을 클리어했습니다.', 'CLEAR_TIME_UNDER', 40, NOW(), NOW());
       