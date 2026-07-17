INSERT INTO games (id, league_id, home_team_id, away_team_id, stadium_id, date, time, deadline, status, home_score, away_score, kbo_game_id, last_synced_at) VALUES
    (1,  1, 1,  8, 1, '2026-06-25', '18:30:00', '2026-06-24', 'SCHEDULED', NULL, NULL, NULL,                NULL),
    (2,  1, 2,  9, 2, '2026-06-25', '18:30:00', '2026-06-24', 'SCHEDULED', NULL, NULL, NULL,                NULL),
    (3,  1, 3,  6, 3, '2026-06-26', '18:30:00', '2026-06-25', 'SCHEDULED', NULL, NULL, NULL,                NULL),
    (4,  1, 4, 10, 4, '2026-06-26', '18:30:00', '2026-06-25', 'SCHEDULED', NULL, NULL, NULL,                NULL),
    (5,  1, 5,  7, 5, '2026-06-27', '18:30:00', '2026-06-26', 'SCHEDULED', NULL, NULL, NULL,                NULL),
    (8,  1, 2,  7, 1, '2026-07-01', '18:30:00', '2026-06-30', 'SCHEDULED', NULL, NULL, '20260701OBLT0', TIMESTAMP '2026-06-22 03:52:41.283667'),
    (18, 1, 1,  8, 1, '2026-07-03', '18:30:00', '2026-07-02', 'SCHEDULED', NULL, NULL, '20260703LGHH0', TIMESTAMP '2026-06-22 03:52:41.283667'),
    (48, 1, 1, 10, 1, '2026-07-16', '18:30:00', '2026-07-15', 'SCHEDULED', NULL, NULL, '20260716LGKT0', TIMESTAMP '2026-06-22 03:52:41.283667'),
    (68, 1, 1,  9, 1, '2026-07-21', '18:30:00', '2026-07-20', 'SCHEDULED', NULL, NULL, '20260721LGNC0', TIMESTAMP '2026-06-22 03:52:41.283667'),
    (83, 1, 2,  5, 1, '2026-07-24', '18:30:00', '2026-07-23', 'SCHEDULED', NULL, NULL, '20260724OBSS0', TIMESTAMP '2026-06-22 03:52:41.283667')
ON CONFLICT (id) DO NOTHING;



INSERT INTO matches (id, game_id, status, matched_at, expires_at, created_at) VALUES
    (1, 8,  'chatting',  TIMESTAMP '2026-06-22 10:00:00', TIMESTAMP '2026-06-23 09:00:00', TIMESTAMP '2026-06-22 10:00:00'),
    (2, 48, 'game_done', TIMESTAMP '2026-06-22 11:00:00', TIMESTAMP '2026-06-23 10:00:00', TIMESTAMP '2026-06-22 11:00:00'),
    (3, 68, 'reviewed',  TIMESTAMP '2026-06-22 12:00:00', TIMESTAMP '2026-06-23 11:00:00', TIMESTAMP '2026-06-22 12:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO match_applications
    (id, member_id, game_id, game_date, match_id, matched_member_id, matched_at, expires_at, response, party_size, status, applied_at, cancelled_at)
VALUES
    (1,  1,  1, DATE '2026-06-25', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-06-22 09:00:00', NULL),
    (2,  1,  3, DATE '2026-06-26', NULL, 2,    TIMESTAMP '2026-06-22 09:30:00', TIMESTAMP '2026-06-23 08:30:00', NULL,       1, 'matched',   TIMESTAMP '2026-06-22 09:10:00', NULL),
    (3,  2,  3, DATE '2026-06-26', NULL, 1,    TIMESTAMP '2026-06-22 09:30:00', TIMESTAMP '2026-06-23 08:30:00', NULL,       1, 'matched',   TIMESTAMP '2026-06-22 09:20:00', NULL),
    (4,  1,  8, DATE '2026-07-01', 1,    8,    TIMESTAMP '2026-06-22 10:00:00', TIMESTAMP '2026-06-23 09:00:00', 'accepted', 1, 'chatting',  TIMESTAMP '2026-06-22 09:40:00', NULL),
    (5,  8,  8, DATE '2026-07-01', 1,    1,    TIMESTAMP '2026-06-22 10:00:00', TIMESTAMP '2026-06-23 09:00:00', 'accepted', 1, 'chatting',  TIMESTAMP '2026-06-22 09:45:00', NULL),
    (6,  1, 18, DATE '2026-07-03', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'cancelled', TIMESTAMP '2026-06-22 10:10:00', TIMESTAMP '2026-06-22 10:20:00'),
    (7,  1, 48, DATE '2026-07-16', 2,    6,    TIMESTAMP '2026-06-22 11:00:00', TIMESTAMP '2026-06-23 10:00:00', 'accepted', 1, 'game_done', TIMESTAMP '2026-06-22 10:30:00', NULL),
    (8,  6, 48, DATE '2026-07-16', 2,    1,    TIMESTAMP '2026-06-22 11:00:00', TIMESTAMP '2026-06-23 10:00:00', 'accepted', 1, 'game_done', TIMESTAMP '2026-06-22 10:35:00', NULL),
    (9,  1, 68, DATE '2026-07-21', 3,    7,    TIMESTAMP '2026-06-22 12:00:00', TIMESTAMP '2026-06-23 11:00:00', 'accepted', 1, 'reviewed',  TIMESTAMP '2026-06-22 11:30:00', NULL),
    (10, 7, 68, DATE '2026-07-21', 3,    1,    TIMESTAMP '2026-06-22 12:00:00', TIMESTAMP '2026-06-23 11:00:00', 'accepted', 1, 'reviewed',  TIMESTAMP '2026-06-22 11:35:00', NULL),
    (11, 10, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:00:00', NULL),
    (12, 11, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:01:00', NULL),
    (13, 12, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:02:00', NULL),
    (14, 13, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:03:00', NULL),
    (15, 14, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:04:00', NULL),
    (16, 15, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:05:00', NULL),
    (17, 16, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:06:00', NULL),
    (18, 17, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:07:00', NULL),
    (19, 18, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:08:00', NULL),
    (20, 19, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:09:00', NULL),
    (21, 20, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:10:00', NULL),
    (22, 21, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:11:00', NULL),
    (23, 22, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:12:00', NULL),
    (24, 23, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:13:00', NULL),
    (25, 24, 83, DATE '2026-07-24', NULL, NULL, NULL,                         NULL,                         NULL,       1, 'waiting',   TIMESTAMP '2026-07-17 09:14:00', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO match_participants (match_id, member_id, application_id, response, joined_at) VALUES
    (1, 1, 4,  'accepted', TIMESTAMP '2026-06-22 10:00:00'),
    (1, 8, 5,  'accepted', TIMESTAMP '2026-06-22 10:00:00'),
    (2, 1, 7,  'accepted', TIMESTAMP '2026-06-22 11:00:00'),
    (2, 6, 8,  'accepted', TIMESTAMP '2026-06-22 11:00:00'),
    (3, 1, 9,  'accepted', TIMESTAMP '2026-06-22 12:00:00'),
    (3, 7, 10, 'accepted', TIMESTAMP '2026-06-22 12:00:00')
ON CONFLICT DO NOTHING;

INSERT INTO chat_messages (id, match_id, sender_id, text, type, sent_at) VALUES
    (1, 1, NULL, '매칭이 확정됐어요. 채팅방에서 일정을 조율해보세요.', 'system', TIMESTAMP '2026-06-22 10:00:00'),
    (2, 1, 8,    '안녕하세요! 경기 당일 어디서 만날까요?',              'text',   TIMESTAMP '2026-06-22 10:05:00'),
    (3, 2, NULL, '직관이 완료됐어요. 후기를 남겨보세요.',                'system', TIMESTAMP '2026-07-16 22:00:00'),
    (4, 3, NULL, '후기 작성이 완료됐어요.',                              'system', TIMESTAMP '2026-07-21 22:00:00')
ON CONFLICT (id) DO NOTHING;
