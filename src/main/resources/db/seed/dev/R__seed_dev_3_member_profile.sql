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

INSERT INTO teams (id, sport_id, league_id, name, short_name, kbo_code, emblem_image_key, primary_color_hex) VALUES
    (1,  1, 1, 'LG 트윈스',    'LG',   'LG',   'images/team-logo/lg.png',      '#C30452'),
    (2,  1, 1, '두산 베어스',  '두산', '두산', 'images/team-logo/doosan.png',  '#131230'),
    (3,  1, 1, '키움 히어로즈','키움', '키움', 'images/team-logo/kiwoom.png',  '#570514'),
    (4,  1, 1, 'KIA 타이거즈', 'KIA',  'KIA',  'images/team-logo/kia.png',     '#EA0029'),
    (5,  1, 1, '삼성 라이온즈','삼성', '삼성', 'images/team-logo/samsung.png', '#074CA1'),
    (6,  1, 1, 'SSG 랜더스',   'SSG',  'SSG',  'images/team-logo/ssg.png',     '#CE0E2D'),
    (7,  1, 1, '롯데 자이언츠','롯데', '롯데', 'images/team-logo/lotte.png',   '#041E42'),
    (8,  1, 1, '한화 이글스',  '한화', '한화', 'images/team-logo/hanhwa.png',  '#FC4E00'),
    (9,  1, 1, 'NC 다이노스',  'NC',   'NC',   'images/team-logo/nc.png',      '#315288'),
    (10, 1, 1, 'KT 위즈',      'KT',   'KT',   'images/team-logo/kt.png',      '#000000')
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
    (8, TRUE, '서울 마포구 합정동',   37.549463, 126.913739, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (10, TRUE, '서울 송파구 잠실동',  37.512257, 127.071901, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (11, TRUE, '서울 강동구 성내동',  37.530124, 127.123770, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (12, TRUE, '서울 광진구 구의동',  37.538484, 127.082293, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (13, TRUE, '서울 서초구 반포동',  37.504598, 127.011399, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (14, TRUE, '서울 송파구 석촌동',  37.505738, 127.106179, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (15, TRUE, '서울 강남구 대치동',  37.494500, 127.063000, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (16, TRUE, '서울 성동구 성수동',  37.544579, 127.055961, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (17, TRUE, '서울 용산구 한남동',  37.534450, 127.005250, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (18, TRUE, '서울 동작구 사당동',  37.476559, 126.981633, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (19, TRUE, '서울 송파구 방이동',  37.514543, 127.112810, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (20, TRUE, '서울 중구 신당동',    37.560247, 127.013925, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (21, TRUE, '서울 강서구 마곡동',  37.566733, 126.829512, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (22, TRUE, '서울 은평구 불광동',  37.610126, 126.929348, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (23, TRUE, '서울 관악구 봉천동',  37.482568, 126.941531, CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (24, TRUE, '서울 송파구 가락동',  37.495465, 127.121861, CURRENT_TIMESTAMP - INTERVAL '1 days')
ON CONFLICT DO NOTHING;

INSERT INTO member_preferences (member_id, gender_pref, age_pref, smoking_pref, distance_km) VALUES
    (1, 'ANY',  'SIMILAR', 'NON_SMOKER', 20),
    (2, 'ANY',  'ANY',     'ANY',        30),
    (3, 'SAME', 'SIMILAR', 'NON_SMOKER', 15),
    (4, 'SAME', 'ANY',     'ANY',        25),
    (5, 'ANY',  'SIMILAR', 'NON_SMOKER', 10),
    (6, 'SAME', 'SIMILAR', 'ANY',        50),
    (7, 'ANY',  'SIMILAR', 'NON_SMOKER', 20),
    (8, 'ANY',  'ANY',     'ANY',        25),
    (10, 'ANY',  'SIMILAR', 'NON_SMOKER', 20),
    (11, 'ANY',  'ANY',     'ANY',        30),
    (12, 'SAME', 'SIMILAR', 'NON_SMOKER', 15),
    (13, 'ANY',  'ANY',     'NON_SMOKER', 25),
    (14, 'ANY',  'SIMILAR', 'ANY',        20),
    (15, 'SAME', 'ANY',     'NON_SMOKER', 30),
    (16, 'ANY',  'SIMILAR', 'NON_SMOKER', 20),
    (17, 'ANY',  'ANY',     'ANY',        35),
    (18, 'SAME', 'SIMILAR', 'NON_SMOKER', 15),
    (19, 'ANY',  'ANY',     'ANY',        25),
    (20, 'ANY',  'SIMILAR', 'NON_SMOKER', 20),
    (21, 'ANY',  'ANY',     'NON_SMOKER', 40),
    (22, 'SAME', 'SIMILAR', 'NON_SMOKER', 20),
    (23, 'ANY',  'ANY',     'ANY',        30),
    (24, 'ANY',  'SIMILAR', 'NON_SMOKER', 20)
ON CONFLICT DO NOTHING;

INSERT INTO member_stats (member_id, match_count, rating, trust_score, coupon_count, priority_pass_count) VALUES
    (1, 10, 4.3, 88, 3, 0),
    (2, 7, 4.6, 92,  4, 1),
    (3, 2, 5.0, 100, 2, 0),
    (4, 3, 4.2, 80,  1, 0),
    (5, 4, 4.9, 95,  3, 0),
    (6, 3, 4.3, 78,  2, 0),
    (7, 6, 4.7, 91,  2, 1),
    (8, 5, 4.5, 87,  1, 0),
    (10, 4, 4.8, 94, 1, 0),
    (11, 8, 4.1, 81, 2, 0),
    (12, 3, 4.7, 90, 1, 0),
    (13, 12, 4.4, 86, 3, 1),
    (14, 2, 5.0, 98, 1, 0),
    (15, 6, 4.6, 89, 2, 0),
    (16, 5, 4.9, 96, 2, 1),
    (17, 9, 4.2, 83, 1, 0),
    (18, 1, 5.0, 100, 1, 0),
    (19, 7, 4.3, 84, 2, 0),
    (20, 4, 4.8, 93, 1, 0),
    (21, 10, 4.0, 80, 3, 0),
    (22, 2, 4.7, 91, 1, 0),
    (23, 6, 4.5, 88, 2, 0),
    (24, 5, 4.9, 95, 1, 1)
ON CONFLICT DO NOTHING;

INSERT INTO member_styles (member_id, favorite_team_id, personality, talk_style, smoking_status) VALUES
    (1, 1, 'TENSION',     'TALKATIVE', 'NON_SMOKER'),
    (2, 2, 'CALM',        'MODERATE',  'NON_SMOKER'),
    (3, 3, 'PLANNER',     'QUIET',     'NON_SMOKER'),
    (4, 4, 'TENSION',     'MODERATE',  'SMOKER'),
    (5, 8, 'CALM',        'QUIET',     'NON_SMOKER'),
    (6, 1, 'SPONTANEOUS', 'TALKATIVE', 'NON_SMOKER'),
    (7, 1, 'PLANNER',     'MODERATE',  'NON_SMOKER'),
    (8, 8, 'CALM',        'TALKATIVE', 'NON_SMOKER'),
    (10, 1, 'TENSION',     'TALKATIVE', 'NON_SMOKER'),
    (11, 5, 'SPONTANEOUS', 'MODERATE',  'NON_SMOKER'),
    (12, 7, 'CALM',        'TALKATIVE', 'NON_SMOKER'),
    (13, 3, 'PLANNER',     'MODERATE',  'NON_SMOKER'),
    (14, 6, 'SPONTANEOUS', 'TALKATIVE', 'NON_SMOKER'),
    (15, 8, 'TENSION',     'MODERATE',  'NON_SMOKER'),
    (16, 10, 'PLANNER',    'QUIET',     'NON_SMOKER'),
    (17, 9, 'CALM',        'QUIET',     'NON_SMOKER'),
    (18, 4, 'TENSION',     'TALKATIVE', 'NON_SMOKER'),
    (19, 1, 'SPONTANEOUS', 'MODERATE',  'NON_SMOKER'),
    (20, 2, 'CALM',        'TALKATIVE', 'NON_SMOKER'),
    (21, 5, 'PLANNER',     'QUIET',     'NON_SMOKER'),
    (22, 1, 'CALM',        'MODERATE',  'NON_SMOKER'),
    (23, 7, 'SPONTANEOUS', 'TALKATIVE', 'NON_SMOKER'),
    (24, 1, 'TENSION',     'MODERATE',  'NON_SMOKER')
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
    (8, 'CHEER'),
    (10, 'CHEER'),
    (10, 'FOOD'),
    (11, 'ENJOY'),
    (12, 'FOOD'),
    (12, 'ENJOY'),
    (13, 'FOCUS'),
    (14, 'CHEER'),
    (14, 'ENJOY'),
    (15, 'CHEER'),
    (16, 'FOCUS'),
    (16, 'ENJOY'),
    (17, 'FOCUS'),
    (18, 'CHEER'),
    (19, 'FOOD'),
    (19, 'CHEER'),
    (20, 'ENJOY'),
    (20, 'CHEER'),
    (21, 'FOCUS'),
    (22, 'ENJOY'),
    (23, 'CHEER'),
    (23, 'FOOD'),
    (24, 'CHEER')
ON CONFLICT DO NOTHING;

INSERT INTO member_league_profiles (member_id, league_id, favorite_team_id, team_pref, fan_level) VALUES
    (1, 1, 1, 'SAME', 'CORE'),
    (2, 1, 2, 'ANY',  'LIGHT'),
    (3, 1, 3, 'SAME', 'LIGHT'),
    (4, 1, 4, 'ANY',  'LIGHT'),
    (5, 1, 8, 'ANY',  'BEGINNER'),
    (6, 1, 1, 'SAME', 'CORE'),
    (7, 1, 1, 'SAME', 'CORE'),
    (8, 1, 8, 'ANY',  'LIGHT'),
    (10, 1, 1, 'SAME', 'CORE'),
    (11, 1, 5, 'ANY',  'LIGHT'),
    (12, 1, 7, 'ANY',  'LIGHT'),
    (13, 1, 3, 'ANY',  'CORE'),
    (14, 1, 6, 'ANY',  'LIGHT'),
    (15, 1, 8, 'SAME', 'CORE'),
    (16, 1, 10, 'ANY', 'LIGHT'),
    (17, 1, 9, 'ANY',  'CORE'),
    (18, 1, 4, 'SAME', 'BEGINNER'),
    (19, 1, 1, 'ANY',  'LIGHT'),
    (20, 1, 2, 'ANY',  'LIGHT'),
    (21, 1, 5, 'SAME', 'CORE'),
    (22, 1, 1, 'SAME', 'LIGHT'),
    (23, 1, 7, 'ANY',  'BEGINNER'),
    (24, 1, 1, 'ANY',  'CORE')
ON CONFLICT DO NOTHING;

INSERT INTO member_league_watch_styles (member_id, league_id, watch_style)
SELECT member_id, 1, watch_style
FROM member_watch_styles
ON CONFLICT DO NOTHING;

INSERT INTO member_league_seat_zones (member_id, league_id, seat_zone) VALUES
    (1, 1, 'CLOSE'),
    (2, 1, 'MIDDLE'),
    (3, 1, 'ANY'),
    (4, 1, 'FAR'),
    (5, 1, 'MIDDLE'),
    (6, 1, 'CLOSE'),
    (7, 1, 'CLOSE'),
    (8, 1, 'MIDDLE'),
    (10, 1, 'CLOSE'),
    (11, 1, 'ANY'),
    (12, 1, 'MIDDLE'),
    (13, 1, 'MIDDLE'),
    (14, 1, 'CLOSE'),
    (15, 1, 'CLOSE'),
    (16, 1, 'MIDDLE'),
    (17, 1, 'FAR'),
    (18, 1, 'CLOSE'),
    (19, 1, 'MIDDLE'),
    (20, 1, 'ANY'),
    (21, 1, 'FAR'),
    (22, 1, 'MIDDLE'),
    (23, 1, 'ANY'),
    (24, 1, 'CLOSE')
ON CONFLICT DO NOTHING;

INSERT INTO notification_settings (member_id, match_request, match_schedule, chat, review, marketing) VALUES
    (1, TRUE,  TRUE,  TRUE,  TRUE, FALSE),
    (2, TRUE,  TRUE,  TRUE,  TRUE, TRUE),
    (3, TRUE,  FALSE, TRUE,  TRUE, FALSE),
    (4, TRUE,  TRUE,  TRUE,  TRUE, TRUE),
    (5, FALSE, TRUE,  TRUE,  FALSE, FALSE),
    (6, TRUE,  TRUE,  FALSE, TRUE, FALSE),
    (7, TRUE,  TRUE,  TRUE,  TRUE, TRUE),
    (8, TRUE,  TRUE,  TRUE,  TRUE, FALSE),
    (10, TRUE, TRUE,  TRUE,  TRUE, FALSE),
    (11, TRUE, TRUE,  TRUE,  TRUE, FALSE),
    (12, TRUE, TRUE,  TRUE,  TRUE, TRUE),
    (13, TRUE, TRUE,  TRUE,  TRUE, FALSE),
    (14, TRUE, TRUE,  TRUE,  TRUE, TRUE),
    (15, TRUE, TRUE,  TRUE,  TRUE, FALSE),
    (16, TRUE, TRUE,  TRUE,  TRUE, TRUE),
    (17, TRUE, TRUE,  TRUE,  TRUE, FALSE),
    (18, TRUE, TRUE,  TRUE,  TRUE, FALSE),
    (19, TRUE, TRUE,  TRUE,  TRUE, TRUE),
    (20, TRUE, TRUE,  TRUE,  TRUE, FALSE),
    (21, TRUE, TRUE,  TRUE,  TRUE, FALSE),
    (22, TRUE, TRUE,  TRUE,  TRUE, TRUE),
    (23, TRUE, TRUE,  TRUE,  TRUE, FALSE),
    (24, TRUE, TRUE,  TRUE,  TRUE, TRUE)
ON CONFLICT DO NOTHING;
