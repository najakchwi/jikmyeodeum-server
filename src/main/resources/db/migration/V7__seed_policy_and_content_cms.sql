INSERT INTO terms (code, version, title, content_key, content, required, valid_days, effective_at)
SELECT 'service', '2026-06-21', '이용약관', NULL,
       '레츠포츠 서비스 이용 조건과 회원의 권리 및 의무를 규정합니다.',
       TRUE, NULL, TIMESTAMP '2026-06-21 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE code = 'service' AND version = '2026-06-21');

INSERT INTO terms (code, version, title, content_key, content, required, valid_days, effective_at)
SELECT 'privacy', '2026-06-21', '개인정보처리방침', NULL,
       '회원 식별과 매칭 서비스 제공을 위해 필요한 개인정보 처리 기준을 안내합니다.',
       TRUE, NULL, TIMESTAMP '2026-06-21 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE code = 'privacy' AND version = '2026-06-21');

INSERT INTO terms (code, version, title, content_key, content, required, valid_days, effective_at)
SELECT 'location', '2026-06-21', '위치기반서비스 이용약관', NULL,
       '거리 기반 매칭과 위치 인증을 위한 위치기반서비스 이용 조건을 규정합니다.',
       TRUE, NULL, TIMESTAMP '2026-06-21 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE code = 'location' AND version = '2026-06-21');

INSERT INTO terms (code, version, title, content_key, content, required, valid_days, effective_at)
SELECT 'age14', '2026-06-21', '만 14세 이상 확인', NULL,
       '레츠포츠는 만 14세 이상만 가입할 수 있으며, 가입자는 만 14세 이상임을 확인합니다.',
       TRUE, NULL, TIMESTAMP '2026-06-21 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE code = 'age14' AND version = '2026-06-21');

INSERT INTO terms (code, version, title, content_key, content, required, valid_days, effective_at)
SELECT 'marketing', '2026-06-21', '마케팅 정보 수신 동의', NULL,
       '이벤트, 혜택, 서비스 소식 등 마케팅 정보를 앱 푸시 또는 알림으로 받을 수 있습니다.',
       FALSE, 365, TIMESTAMP '2026-06-21 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE code = 'marketing' AND version = '2026-06-21');

UPDATE teams
SET emblem_image_key = CASE short_name
        WHEN 'LG' THEN 'images/team-logo/lg.png'
        WHEN 'SSG' THEN 'images/team-logo/ssg.png'
        WHEN 'KIA' THEN 'images/team-logo/kia.png'
        WHEN '삼성' THEN 'images/team-logo/samsung.png'
        WHEN '롯데' THEN 'images/team-logo/lotte.png'
        WHEN '한화' THEN 'images/team-logo/hanwha.png'
        WHEN 'KT' THEN 'images/team-logo/kt.png'
        WHEN '두산' THEN 'images/team-logo/doosan.png'
        WHEN 'NC' THEN 'images/team-logo/nc.png'
        WHEN '키움' THEN 'images/team-logo/kiwoom.png'
        ELSE emblem_image_key
    END,
    primary_color_hex = CASE short_name
        WHEN 'LG' THEN '#C30452'
        WHEN 'SSG' THEN '#CE0E2D'
        WHEN 'KIA' THEN '#EA0029'
        WHEN '삼성' THEN '#074CA1'
        WHEN '롯데' THEN '#041E42'
        WHEN '한화' THEN '#FC4E00'
        WHEN 'KT' THEN '#000000'
        WHEN '두산' THEN '#131230'
        WHEN 'NC' THEN '#315288'
        WHEN '키움' THEN '#570514'
        ELSE primary_color_hex
    END;

INSERT INTO banners (code, title, image_key, link_url, display_order, active, starts_at, ends_at)
SELECT 'home_main_1', '직관 메이트 찾기', 'images/banners/home-main-1.png', NULL, 1, TRUE, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM banners WHERE code = 'home_main_1');

INSERT INTO banners (code, title, image_key, link_url, display_order, active, starts_at, ends_at)
SELECT 'home_main_2', '응원 스타일 매칭', 'images/banners/home-main-2.png', NULL, 2, TRUE, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM banners WHERE code = 'home_main_2');

INSERT INTO faqs (code, category, question, answer, display_order, active)
SELECT 'matching_flow', 'matching', '매칭은 어떻게 진행되나요?',
       '같은 경기에 신청한 사용자 중 응원팀, 관람 스타일, 성격, 매칭 허용 거리, 신뢰도 점수 등을 종합적으로 고려해 매칭합니다. 상대가 배치되면 채팅방이 자동으로 열립니다.',
       1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM faqs WHERE code = 'matching_flow');

INSERT INTO faqs (code, category, question, answer, display_order, active)
SELECT 'cancel_application', 'matching', '신청한 매칭을 취소할 수 있나요?',
       '상대 배치 전에는 불이익 없이 취소할 수 있습니다. 다만 매칭이 확정된 이후 정당한 사유 없이 반복적으로 취소하면 신뢰도 점수가 차감될 수 있습니다.',
       2, TRUE
WHERE NOT EXISTS (SELECT 1 FROM faqs WHERE code = 'cancel_application');

INSERT INTO faqs (code, category, question, answer, display_order, active)
SELECT 'report_user', 'safety', '신고는 어떻게 하나요?',
       '채팅방 또는 매칭 결과 화면 우측 메뉴에서 신고 사유를 선택해 접수할 수 있습니다. 접수된 신고는 운영팀 검토 후 처리됩니다.',
       3, TRUE
WHERE NOT EXISTS (SELECT 1 FROM faqs WHERE code = 'report_user');

INSERT INTO avatar_presets (code, name, image_key, display_order, active)
SELECT 'baseball_cap_green', '초록 야구모자', 'images/avatars/presets/baseball-cap-green.png', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM avatar_presets WHERE code = 'baseball_cap_green');

INSERT INTO avatar_presets (code, name, image_key, display_order, active)
SELECT 'baseball_cap_red', '빨강 야구모자', 'images/avatars/presets/baseball-cap-red.png', 2, TRUE
WHERE NOT EXISTS (SELECT 1 FROM avatar_presets WHERE code = 'baseball_cap_red');

INSERT INTO content_assets (type, code, name, object_key, display_order, active)
SELECT 'icon', 'match', '매칭 아이콘', 'images/icons/match.png', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM content_assets WHERE type = 'icon' AND code = 'match');

INSERT INTO content_assets (type, code, name, object_key, display_order, active)
SELECT 'general', 'default_avatar', '기본 아바타', 'images/general/default-avatar.png', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM content_assets WHERE type = 'general' AND code = 'default_avatar');
