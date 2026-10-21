
-- ============================================================================
-- 1. PROPERTIES (Physical Managed Real Estate Assets)
-- ============================================================================
CREATE TABLE properties (
                            property_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(255) NOT NULL,
                            street_address VARCHAR(255) NOT NULL,
                            city VARCHAR(100) NOT NULL,
                            state VARCHAR(50) NOT NULL,
                            zip_code VARCHAR(20) NOT NULL,
                            status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);