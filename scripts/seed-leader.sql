-- TestFlow Lite - Reference Leader Seed Script
-- NOTE: Automatic initialization is handled at application runtime by LeaderSeeder.java using Spring Boot.
-- This file is kept for manual SQL reference or direct database testing only.

USE testhub_db;

-- BCrypt hash below corresponds to default password: Leader@123456
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
