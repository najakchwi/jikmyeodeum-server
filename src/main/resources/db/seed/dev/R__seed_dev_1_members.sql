INSERT INTO members (id, auth_id, phone, phone_verified_at, nickname, birth_year, gender, profile_image_key, bio, expo_push_token, welcome_notified, role, created_at, updated_at) VALUES
    (1, NULL, '01011112222', CURRENT_TIMESTAMP, '잠실직관러',  1997, 'FEMALE', NULL, 'LG 홈경기 직관 좋아해요.',                     'ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]', TRUE,  'USER',  CURRENT_TIMESTAMP, NULL),
    (2, NULL, '01022223333', CURRENT_TIMESTAMP, '응원단장',    1995, 'MALE',   NULL, '응원가 크게 부르는 편입니다.',                  'ExponentPushToken[yyyyyyyyyyyyyyyyyyyyyy]', TRUE,  'USER',  CURRENT_TIMESTAMP, NULL),
    (3, NULL, '01044445555', CURRENT_TIMESTAMP, '야구친구',    1999, 'FEMALE', NULL, '처음 보는 분과도 편하게 봐요.',                 NULL,                                         FALSE, 'USER',  CURRENT_TIMESTAMP, NULL),
    (4, NULL, '01033334444', CURRENT_TIMESTAMP, '직관초보',    2000, 'MALE',   NULL, '직관 3번째인데 아직 응원법을 잘 몰라요.',        'ExponentPushToken[zzzzzzzzzzzzzzzzzzzzzz]', TRUE,  'USER',  CURRENT_TIMESTAMP, NULL),
    (5, NULL, '01055556677', CURRENT_TIMESTAMP, '한화팬클럽',  1993, 'FEMALE', NULL, '한화 10년차 팬입니다. 같이 울어줄 분 구해요.',  NULL,                                         FALSE, 'USER',  CURRENT_TIMESTAMP, NULL),
    (6, NULL, '01066667777', CURRENT_TIMESTAMP, '트윈스사랑',  1998, 'MALE',   NULL, 'LG 직관 동행 항상 환영입니다.',                 'ExponentPushToken[wwwwwwwwwwwwwwwwwwwwww]', FALSE, 'USER',  CURRENT_TIMESTAMP, NULL),
    (7, NULL, '01077778888', CURRENT_TIMESTAMP, '오늘직관러',  1996, 'FEMALE', NULL, '오늘 경기 같이 볼 동행을 찾고 있어요.',          'ExponentPushToken[vvvvvvvvvvvvvvvvvvvvvv]', TRUE,  'USER',  CURRENT_TIMESTAMP, NULL),
    (8, NULL, '01088889999', CURRENT_TIMESTAMP, '매칭완료팬',  1994, 'MALE',   NULL, '매칭되면 바로 일정 조율하는 편입니다.',           'ExponentPushToken[uuuuuuuuuuuuuuuuuuuuuu]', TRUE,  'USER',  CURRENT_TIMESTAMP, NULL),
    (9, NULL, '01099990000', CURRENT_TIMESTAMP, '운영자',      NULL, NULL,     NULL, '매칭 기능 테스트용 관리자 계정',                 NULL,                                         TRUE,  'ADMIN', CURRENT_TIMESTAMP, NULL)
ON CONFLICT (id) DO NOTHING;
