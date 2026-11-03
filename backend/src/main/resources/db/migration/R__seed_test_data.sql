-- 1. Seed a Mock Property
INSERT INTO properties (property_uuid, name, street_address, city, state, zip_code, status)
VALUES ('11111111-1111-1111-1111-111111111111', 'Industrial Business Park', '125 Industrial Way', 'Springfield', 'IL', '62701', 'ACTIVE');

-- 2. Seed a Mock Vendor
INSERT INTO vendors (vendor_uuid, vendor_name, billing_phone_number, status, payment_method)
VALUES ('22222222-2222-2222-2222-222222222222', 'Apex Utilities', '555-0192', 'ACTIVE', 'ACH');

-- 3. Map Vendor to Property (Creates the required mapping_uuid record)
INSERT INTO property_vendor_mapping (mapping_uuid, vendor_uuid, property_uuid, vendor_account_number, status, auto_approval_enabled)
VALUES ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'ACT-9483920', 'ACTIVE', FALSE);

-- 4. Seed a Dummy Invoice for your Audit Log Tests
INSERT INTO invoices (invoice_uuid, vendor_supplied_invoice_id, mapping_uuid, amount, invoice_status)
VALUES ('99999999-9999-9999-9999-999999999999', 'INV-2026-001', '33333333-3333-3333-3333-333333333333', 4500.00, 'INGESTED');
