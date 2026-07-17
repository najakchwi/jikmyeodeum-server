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
