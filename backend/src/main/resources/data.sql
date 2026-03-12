-- Seed demo representatives (idempotent via WHERE NOT EXISTS; no unique on external_id)
INSERT INTO representatives (external_id, name, chamber, state, district, party, photo_url, official_url)
SELECT 'rep-1', 'Jane Smith', 'HOUSE', 'CA', '12', 'DEMOCRATIC', NULL, 'https://example.com/jane'
WHERE NOT EXISTS (SELECT 1 FROM representatives WHERE external_id = 'rep-1');

INSERT INTO representatives (external_id, name, chamber, state, district, party, photo_url, official_url)
SELECT 'rep-2', 'John Doe', 'SENATE', 'CA', NULL, 'REPUBLICAN', NULL, 'https://example.com/john'
WHERE NOT EXISTS (SELECT 1 FROM representatives WHERE external_id = 'rep-2');

INSERT INTO representatives (external_id, name, chamber, state, district, party, photo_url, official_url)
SELECT 'rep-3', 'Alex Johnson', 'HOUSE', 'NY', '10', 'DEMOCRATIC', NULL, 'https://example.com/alex'
WHERE NOT EXISTS (SELECT 1 FROM representatives WHERE external_id = 'rep-3');

-- Seed ZIP to representative mappings (unique constraint on zip_code, representative_id)
INSERT INTO zip_representatives (zip_code, representative_id)
SELECT '94110', id FROM representatives WHERE external_id = 'rep-1' LIMIT 1
ON CONFLICT (zip_code, representative_id) DO NOTHING;

INSERT INTO zip_representatives (zip_code, representative_id)
SELECT '94110', id FROM representatives WHERE external_id = 'rep-2' LIMIT 1
ON CONFLICT (zip_code, representative_id) DO NOTHING;

INSERT INTO zip_representatives (zip_code, representative_id)
SELECT '10001', id FROM representatives WHERE external_id = 'rep-3' LIMIT 1
ON CONFLICT (zip_code, representative_id) DO NOTHING;

-- Seed donor summaries (idempotent via WHERE NOT EXISTS)
INSERT INTO donor_summaries (representative_id, cycle_year, source, total_amount, top_industry_1, top_industry_2)
SELECT id, 2024, 'FEC (mock)', 500000.00, 'Finance', 'Healthcare'
FROM representatives r WHERE external_id = 'rep-1'
  AND NOT EXISTS (SELECT 1 FROM donor_summaries ds WHERE ds.representative_id = r.id AND ds.cycle_year = 2024);

INSERT INTO donor_summaries (representative_id, cycle_year, source, total_amount, top_industry_1, top_industry_2)
SELECT id, 2024, 'FEC (mock)', 750000.00, 'Technology', 'Energy'
FROM representatives r WHERE external_id = 'rep-2'
  AND NOT EXISTS (SELECT 1 FROM donor_summaries ds WHERE ds.representative_id = r.id AND ds.cycle_year = 2024);

INSERT INTO donor_summaries (representative_id, cycle_year, source, total_amount, top_industry_1, top_industry_2)
SELECT id, 2024, 'FEC (mock)', 320000.00, 'Labor', 'Education'
FROM representatives r WHERE external_id = 'rep-3'
  AND NOT EXISTS (SELECT 1 FROM donor_summaries ds WHERE ds.representative_id = r.id AND ds.cycle_year = 2024);
