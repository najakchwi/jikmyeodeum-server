-- Dev member profile prerequisites.
INSERT INTO leagues (id, sport_id, code, name) VALUES
    (1, 1, 'KBO', 'KBO 리그')
ON CONFLICT (id) DO NOTHING;

-- ── 구장 (KBO 10개 구단 홈구장) ───────────────────────────────────────────────
-- 잠실은 LG · 두산 공동 홈구장

INSERT INTO stadiums (id, name, kbo_code, address, latitude, longitude) VALUES
    (1, '잠실야구장',              '잠실', '서울 송파구 올림픽로 25',           37.512257, 127.071901),
    (2, '고척스카이돔',            '고척', '서울 구로구 경인로 430',             37.498212, 126.867088),
    (3, '광주-기아챔피언스필드',   '광주', '광주 북구 서림로 10',               35.168388, 126.889562),
    (4, '대구-삼성라이온즈파크',   '대구', '대구 수성구 야구전설로 1',          35.841333, 128.681722),
    (5, '인천-SSG랜더스필드',      '문학', '인천 미추홀구 매소홀로 618',        37.437044, 126.693275),
    (6, '부산-사직야구장',         '사직', '부산 동래구 사직로 45',             35.193989, 129.061366),
    (7, '대전-한화생명이글스파크', '대전', '대전 중구 대종로 373',              36.317069, 127.428083),
    (8, '창원-NC파크',             '창원', '경남 창원시 마산회원구 삼호로 63',  35.222419, 128.582456),
    (9, '수원-KT위즈파크',         '수원', '경기 수원시 장안구 경수대로 893',   37.299783, 127.009567)
ON CONFLICT (id) DO NOTHING;

-- ── 팀 (KBO 10개 구단) ────────────────────────────────────────────────────────
-- short_name 은 클라이언트 TEAM_LOGOS 키와 반드시 일치

INSERT INTO teams (id, sport_id, name, short_name, kbo_code, emblem_image_key, primary_color_hex) VALUES
    (1,  1, 'LG 트윈스',    'LG',   'LG',   'images/team-logo/lg.png',      '#C30452'),
    (2,  1, '두산 베어스',  '두산', '두산', 'images/team-logo/doosan.png',  '#131230'),
    (3,  1, '키움 히어로즈','키움', '키움', 'images/team-logo/kiwoom.png',  '#570514'),
    (4,  1, 'KIA 타이거즈', 'KIA',  'KIA',  'images/team-logo/kia.png',     '#EA0029'),
    (5,  1, '삼성 라이온즈','삼성', '삼성', 'images/team-logo/samsung.png', '#074CA1'),
    (6,  1, 'SSG 랜더스',   'SSG',  'SSG',  'images/team-logo/ssg.png',     '#CE0E2D'),
    (7,  1, '롯데 자이언츠','롯데', '롯데', 'images/team-logo/lotte.png',   '#041E42'),
    (8,  1, '한화 이글스',  '한화', '한화', 'images/team-logo/hanhwa.png',  '#FC4E00'),
    (9,  1, 'NC 다이노스',  'NC',   'NC',   'images/team-logo/nc.png',      '#315288'),
    (10, 1, 'KT 위즈',      'KT',   'KT',   'images/team-logo/kt.png',      '#000000')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('leagues', 'id'), COALESCE((SELECT MAX(id) FROM leagues), 1), (SELECT COUNT(*) > 0 FROM leagues));
SELECT setval(pg_get_serial_sequence('stadiums', 'id'), COALESCE((SELECT MAX(id) FROM stadiums), 1), (SELECT COUNT(*) > 0 FROM stadiums));
SELECT setval(pg_get_serial_sequence('teams', 'id'), COALESCE((SELECT MAX(id) FROM teams), 1), (SELECT COUNT(*) > 0 FROM teams));

-- Dev member profile-related seed.
INSERT INTO member_location_verifications (member_id, verified, address, latitude, longitude, verified_at) VALUES
    (1, TRUE, '서울 송파구 잠실동',   37.512257, 127.071901, CURRENT_TIMESTAMP - INTERVAL '10 days'),
    (2, TRUE, '서울 강남구 삼성동',   37.508615, 127.063252, CURRENT_TIMESTAMP - INTERVAL '9 days'),
    (4, TRUE, '부산 동래구 사직동',   35.193989, 129.061366, CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (5, TRUE, '대전 중구 대흥동',     36.317069, 127.428083, CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (7, TRUE, '서울 송파구 잠실동',   37.512257, 127.071901, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (8, TRUE, '서울 마포구 합정동',   37.549463, 126.913739, CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT DO NOTHING;

INSERT INTO member_preferences (member_id, gender_pref, age_pref, smoking_pref, distance_km) VALUES
    (1, 'ANY',  'SIMILAR', 'NON_SMOKER', 20),
    (2, 'ANY',  'ANY',     'ANY',        30),
    (3, 'SAME', 'SIMILAR', 'NON_SMOKER', 15),
    (4, 'SAME', 'ANY',     'ANY',        25),
    (5, 'ANY',  'SIMILAR', 'NON_SMOKER', 10),
    (6, 'SAME', 'SIMILAR', 'ANY',        50),
    (7, 'ANY',  'SIMILAR', 'NON_SMOKER', 20),
    (8, 'ANY',  'ANY',     'ANY',        25)
ON CONFLICT DO NOTHING;

INSERT INTO member_stats (member_id, match_count, rating, trust_score, coupon_count, priority_pass_count) VALUES
    (1, 10, 4.3, 88, 3, 0),
    (2, 7, 4.6, 92,  4, 1),
    (3, 2, 5.0, 100, 2, 0),
    (4, 3, 4.2, 80,  1, 0),
    (5, 4, 4.9, 95,  3, 0),
    (6, 3, 4.3, 78,  2, 0),
    (7, 6, 4.7, 91,  2, 1),
    (8, 5, 4.5, 87,  1, 0)
ON CONFLICT DO NOTHING;

INSERT INTO member_styles (member_id, favorite_team_id, personality, talk_style, smoking_status) VALUES
    (1, 1, 'TENSION',     'TALKATIVE', 'NON_SMOKER'),
    (2, 2, 'CALM',        'MODERATE',  'NON_SMOKER'),
    (3, 3, 'PLANNER',     'QUIET',     'NON_SMOKER'),
    (4, 4, 'TENSION',     'MODERATE',  'SMOKER'),
    (5, 8, 'CALM',        'QUIET',     'NON_SMOKER'),
    (6, 1, 'SPONTANEOUS', 'TALKATIVE', 'NON_SMOKER'),
    (7, 1, 'PLANNER',     'MODERATE',  'NON_SMOKER'),
    (8, 8, 'CALM',        'TALKATIVE', 'NON_SMOKER')
ON CONFLICT DO NOTHING;

INSERT INTO member_watch_styles (member_id, watch_style) VALUES
    (1, 'CHEER'),
    (1, 'FOOD'),
    (2, 'FOCUS'),
    (2, 'CHEER'),
    (3, 'ENJOY'),
    (4, 'CHEER'),
    (4, 'FOCUS'),
    (5, 'ENJOY'),
    (5, 'FOCUS'),
    (6, 'CHEER'),
    (7, 'CHEER'),
    (7, 'FOOD'),
    (8, 'FOCUS'),
    (8, 'CHEER')
ON CONFLICT DO NOTHING;

INSERT INTO notification_settings (member_id, match_request, match_schedule, chat, review, marketing) VALUES
    (1, TRUE,  TRUE,  TRUE,  TRUE, FALSE),
    (2, TRUE,  TRUE,  TRUE,  TRUE, TRUE),
    (3, TRUE,  FALSE, TRUE,  TRUE, FALSE),
    (4, TRUE,  TRUE,  TRUE,  TRUE, TRUE),
    (5, FALSE, TRUE,  TRUE,  FALSE, FALSE),
    (6, TRUE,  TRUE,  FALSE, TRUE, FALSE),
    (7, TRUE,  TRUE,  TRUE,  TRUE, TRUE),
    (8, TRUE,  TRUE,  TRUE,  TRUE, FALSE)
ON CONFLICT DO NOTHING;
