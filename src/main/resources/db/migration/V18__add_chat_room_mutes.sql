CREATE TABLE chat_room_mutes (
    member_id BIGINT NOT NULL,
    chat_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT pk_chat_room_mutes PRIMARY KEY (member_id, chat_id),
    CONSTRAINT fk_chat_room_mutes_member FOREIGN KEY (member_id) REFERENCES members(id)
);
