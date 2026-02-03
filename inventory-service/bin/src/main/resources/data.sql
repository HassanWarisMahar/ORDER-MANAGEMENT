-- Sample inventory data for testing
INSERT INTO inventory_items (item_code, item_name, available_stock, created_at, updated_at) 
VALUES 
  ('ITEM-001', 'Laptop', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ITEM-002', 'Mouse', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ITEM-003', 'Keyboard', 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ITEM-004', 'Monitor', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('ITEM-005', 'Headphones', 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
