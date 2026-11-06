-- Invoices are created at upload time, before vendor/property are known
ALTER TABLE invoices ALTER COLUMN mapping_uuid DROP NOT NULL;

-- Reject exact duplicate files (also blocks two simultaneous uploads of the same file)
CREATE UNIQUE INDEX uq_invoices_blob_file_sha256 ON invoices (blob_file_sha256);

-- Files are stored under a UUID key, so keep the user's filename for display
ALTER TABLE invoices ADD COLUMN original_filename VARCHAR(255);