INSERT INTO banners (id, code, title, image_key, link_url, display_order, active, starts_at, ends_at) VALUES
    (1, 'home_main_1', '직관 메이트 찾기', 'images/banners/home-main-1.png', NULL, 1, TRUE, NULL, NULL),
    (2, 'home_main_2', '응원 스타일 매칭', 'images/banners/home-main-2.png', NULL, 2, TRUE, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO faqs (id, code, category, question, answer, display_order, active) VALUES
    (1, 'matching_flow', 'matching', '매칭은 어떻게 진행되나요?',
     '같은 경기에 신청한 사용자 중 응원팀, 관람 스타일, 성격, 매칭 허용 거리, 신뢰도 점수 등을 종합적으로 고려해 매칭합니다. 상대가 배치되면 채팅방이 자동으로 열립니다.',
     1, TRUE),
    (2, 'cancel_application', 'matching', '신청한 매칭을 취소할 수 있나요?',
     '상대 배치 전에는 불이익 없이 취소할 수 있습니다. 다만 매칭이 확정된 이후 정당한 사유 없이 반복적으로 취소하면 신뢰도 점수가 차감될 수 있습니다.',
     2, TRUE),
    (3, 'report_user', 'safety', '신고는 어떻게 하나요?',
     '채팅방 또는 매칭 결과 화면 우측 메뉴에서 신고 사유를 선택해 접수할 수 있습니다. 접수된 신고는 운영팀 검토 후 처리됩니다.',
     3, TRUE),
    (4, 'free_service', 'service', '레츠포츠는 무료로 이용할 수 있나요?', '네, 현재 매칭 신청, 채팅, 후기 작성 등 핵심 기능은 모두 무료로 이용할 수 있어요.', 4, true),

    (5, 'multiple_applications', 'matching', '한 번에 여러 경기에 신청할 수 있나요?', '네, 일정이 겹치지 않는다면 여러 경기에 동시에 신청할 수 있어요. 각 신청은 신청내역 화면에서 개별적으로 확인하고 취소할 수 있어요.', 5, true),

    (6, 'matching_delay', 'matching', '매칭이 잘 안 잡혀요. 왜 그런가요?', '같은 경기에 신청한 사용자가 적거나, 응원팀·관람 스타일·매칭 허용 거리 등 설정한 조건이 좁으면 매칭이 늦어질 수 있어요. 마이홈 > 마이스타일에서 선호 조건의 범위를 넓혀보시면 매칭 확률을 높일 수 있어요.', 6, true),

    (7, 'no_show_user', 'matching', '매칭된 상대가 약속 장소에 나타나지 않았어요.', '채팅방 메뉴의 신고하기에서 사유를 선택해 접수해주세요. 접수된 내용은 상대방 신뢰도 점수에 반영되며, 반복될 경우 서비스 이용이 제한될 수 있어요.', 7, true),

    (8, 'block_user', 'safety', '매칭 상대를 차단할 수 있나요?', '현재는 별도의 차단 기능을 제공하지 않고 있어요. 부적절한 이용자는 신고하기를 통해 운영팀에 접수해 주세요.', 8, true),

    (9, 'reported_user', 'safety', '제가 신고를 당했어요. 어떻게 되나요?', '접수된 신고는 운영팀이 채팅 내용 등을 바탕으로 검토하며, 사실 확인 전까지는 불이익이 없어요. 신고 내용에 동의하지 않으시면 1:1 문의를 통해 소명하실 수 있어요.', 9, true),

    (10, 'password_reset', 'account', '비밀번호를 잊어버렸어요. 어떻게 변경하나요?', '현재는 앱 내 비밀번호 찾기 기능이 없어요. 가입하신 휴대폰 번호로 1:1 문의를 주시면 본인 확인 후 재설정을 도와드려요.', 10, true),

    (11, 'location_verification_fail', 'account', '위치 인증이 계속 실패해요.', '휴대폰 설정에서 위치 권한이 허용되어 있는지 확인해주세요. GPS 신호가 약한 실내에서는 실외로 이동한 후 다시 시도하면 인증 성공률이 높아져요.', 11, true),

    (12, 'rejoin_same_phone', 'account', '탈퇴 후 같은 번호로 다시 가입할 수 있나요?', '네, 같은 휴대폰 번호로 다시 가입할 수 있어요. 다만 이전 계정의 매칭 기록, 후기, 신뢰도 점수는 복구되지 않고 새 계정으로 시작돼요.', 12, true),

    (13, 'app_error', 'service', '앱에서 오류가 발생했어요.', '1:1 문의를 통해 발생 화면과 상황을 알려주시면 빠르게 확인해드려요. 가능하다면 캡처 화면을 함께 보내주시면 더 정확하게 파악할 수 있어요.', 13, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO avatar_presets (id, code, name, image_key, display_order, active) VALUES
    (1, 'baseball_cap_green', '초록 야구모자', 'images/avatars/presets/baseball-cap-green.png', 1, TRUE),
    (2, 'baseball_cap_red', '빨강 야구모자', 'images/avatars/presets/baseball-cap-red.png', 2, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_assets (id, type, code, name, object_key, display_order, active) VALUES
    (1, 'icon', 'match', '매칭 아이콘', 'images/icons/match.png', 1, TRUE),
    (2, 'general', 'default_avatar', '기본 아바타', 'images/general/default-avatar.png', 1, TRUE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('banners', 'id'), COALESCE((SELECT MAX(id) FROM banners), 1), (SELECT COUNT(*) > 0 FROM banners));
SELECT setval(pg_get_serial_sequence('faqs', 'id'), COALESCE((SELECT MAX(id) FROM faqs), 1), (SELECT COUNT(*) > 0 FROM faqs));
SELECT setval(pg_get_serial_sequence('avatar_presets', 'id'), COALESCE((SELECT MAX(id) FROM avatar_presets), 1), (SELECT COUNT(*) > 0 FROM avatar_presets));
SELECT setval(pg_get_serial_sequence('content_assets', 'id'), COALESCE((SELECT MAX(id) FROM content_assets), 1), (SELECT COUNT(*) > 0 FROM content_assets));
