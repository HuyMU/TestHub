-- TestFlow Lite - Seed Single Default Leader Account
-- This script runs automatically on MySQL container initialization or can be executed manually.

USE testhub_db;

-- BCrypt hash below corresponds to password: Leader@123456
INSERT INTO users (username, email, password_hash, full_name, role, is_active, created_at)
VALUES (
    'leader',
    'leader@testhub.com',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07Xd0GlM95n9cR8Wiy',
    'System Leader',
    'LEADER',
    true,
    NOW()
)
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    is_active = true;
