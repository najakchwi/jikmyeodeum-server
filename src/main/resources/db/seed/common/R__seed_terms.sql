INSERT INTO terms (id, code, version, title, content_key, content, required, valid_days, effective_at) VALUES
    (1, 'service',   '2026-06-21', '이용약관',               'terms/service/2026-06-21.txt',  '레츠포츠 서비스 이용 조건과 회원의 권리 및 의무를 규정합니다.', TRUE,  NULL, TIMESTAMP '2026-06-21 00:00:00'),
    (2, 'privacy',   '2026-06-21', '개인정보처리방침',        'terms/privacy/2026-06-21.txt',  '회원 식별과 매칭 서비스 제공을 위해 필요한 개인정보 처리 기준을 안내합니다.', TRUE,  NULL, TIMESTAMP '2026-06-21 00:00:00'),
    (3, 'location',  '2026-06-21', '위치기반서비스 이용약관', 'terms/location/2026-06-21.txt', '거리 기반 매칭과 위치 인증을 위한 위치기반서비스 이용 조건을 규정합니다.', TRUE,  NULL, TIMESTAMP '2026-06-21 00:00:00'),
    (4, 'age14',     '2026-06-21', '만 14세 이상 확인',       NULL,                                      '레츠포츠는 만 14세 이상만 가입할 수 있으며, 가입자는 만 14세 이상임을 확인합니다.', TRUE,  NULL, TIMESTAMP '2026-06-21 00:00:00'),
    (5, 'marketing', '2026-06-21', '마케팅 정보 수신 동의',   NULL,                                      '이벤트, 혜택, 서비스 소식 등 마케팅 정보를 앱 푸시 또는 알림으로 받을 수 있습니다.', FALSE, 365, TIMESTAMP '2026-06-21 00:00:00'),
    -- 2026-07-16 개정판: 연령 조항·연락처·보유기간 등 명시(스토어 심사 대응). 기존 06-21 행은 이력 보존을 위해 유지, 새 버전은 새 row로 발행 → 필수 3종 재동의(NEW_VERSION) 트리거.
    (6, 'service',   '2026-07-16', '이용약관',               'terms/service/2026-07-16.txt',  '레츠포츠 서비스 이용 조건과 회원의 권리·의무, 만 14세 미만 가입 제한, 신고·차단 및 계정 삭제 절차를 규정합니다.', TRUE,  NULL, TIMESTAMP '2026-07-16 00:00:00'),
    (7, 'privacy',   '2026-07-16', '개인정보처리방침',        'terms/privacy/2026-07-16.txt',  '수집 항목·이용 목적·보유기간·제3자 제공·위탁·이용자 권리 및 만 14세 미만 아동 개인정보 처리 기준을 안내합니다.', TRUE,  NULL, TIMESTAMP '2026-07-16 00:00:00'),
    (8, 'location',  '2026-07-16', '위치기반서비스 이용약관', 'terms/location/2026-07-16.txt', '거리 기반 매칭·위치 인증을 위한 개인위치정보 처리 기준과 개인위치정보주체의 권리를 규정합니다.', TRUE,  NULL, TIMESTAMP '2026-07-16 00:00:00')
ON CONFLICT (id) DO UPDATE SET
    code = EXCLUDED.code,
    version = EXCLUDED.version,
    title = EXCLUDED.title,
    content_key = EXCLUDED.content_key,
    content = EXCLUDED.content,
    required = EXCLUDED.required,
    valid_days = EXCLUDED.valid_days,
    effective_at = EXCLUDED.effective_at;

SELECT setval(pg_get_serial_sequence('terms', 'id'), COALESCE((SELECT MAX(id) FROM terms), 1), (SELECT COUNT(*) > 0 FROM terms));
