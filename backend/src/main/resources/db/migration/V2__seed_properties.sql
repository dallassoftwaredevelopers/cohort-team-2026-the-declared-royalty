
INSERT INTO properties (
    property_id,
    name,
    street_address,
    city,
    state,
    zip_code,
    status
) VALUES (
             '11111111-1111-1111-1111-111111111101',
             'Industrial Business Park',
             '125 Industrial Way',
             'Springfield',
             'IL',
             '62701',
             'ACTIVE'
         ) ON CONFLICT (property_id) DO NOTHING;