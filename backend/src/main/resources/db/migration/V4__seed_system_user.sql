-- Stand-in actor for automated actions until real authentication exists
INSERT INTO users (user_uuid, user_name, email, role, status)
VALUES ('00000000-0000-0000-0000-000000000001',
        'System',
        'system@invoice-automation.local',
        'ADMIN',
        'ACTIVE')
    ON CONFLICT (user_uuid) DO NOTHING;