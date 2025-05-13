-- Sample data for the FUNDRAISING_EVENT table
INSERT INTO FUNDRAISING_EVENT (id, name, account_currency, account_balance) VALUES
('11111111-1111-1111-1111-111111111111', 'Support for Children in Need', 'PLN', 1500.75),
('22222222-2222-2222-2222-222222222222', 'Saving Homeless Cats', 'EUR', 750.50),
('33333333-3333-3333-3333-333333333333', 'New Equipment for Local Hospital', 'USD', 10200.00),
('44444444-4444-4444-4444-444444444444', 'Scholarships for Talented Youth', 'PLN', 0.00);

-- Sample data for the COLLECTION_BOX table
-- fundraising_event_id refers to the ID from the FUNDRAISING_EVENT table
INSERT INTO COLLECTION_BOX (id, fundraising_event_id) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111'), -- Box for "Support for Children in Need"
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111'), -- Second box for "Support for Children in Need"
('cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222'), -- Box for "Saving Homeless Cats"
('dddddddd-dddd-dddd-dddd-dddddddddddd', '33333333-3333-3333-3333-333333333333'), -- Box for "New Equipment for Local Hospital"
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', NULL);                                   -- Box not assigned to any fundraising event

-- Sample data for the COLLECTION_BOX_AMOUNTS table
-- collection_box_id refers to the ID from the COLLECTION_BOX table
-- Box 1 (aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa)
INSERT INTO COLLECTION_BOX_AMOUNTS (collection_box_id, currency, amount) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'PLN', 120.50),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'EUR', 15.00);

-- Box 2 (bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb)
INSERT INTO COLLECTION_BOX_AMOUNTS (collection_box_id, currency, amount) VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'PLN', 75.25);

-- Box 3 (cccccccc-cccc-cccc-cccc-cccccccccccc)
INSERT INTO COLLECTION_BOX_AMOUNTS (collection_box_id, currency, amount) VALUES
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'EUR', 220.70),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'USD', 50.00);

-- Box 4 (dddddddd-dddd-dddd-dddd-dddddddddddd) - empty
-- You can explicitly insert zero, or simply not insert records for empty currencies
INSERT INTO COLLECTION_BOX_AMOUNTS (collection_box_id, currency, amount) VALUES
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'USD', 0.00);

-- Box 5 (eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee) - unassigned, but may contain funds
INSERT INTO COLLECTION_BOX_AMOUNTS (collection_box_id, currency, amount) VALUES
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'PLN', 10.00);