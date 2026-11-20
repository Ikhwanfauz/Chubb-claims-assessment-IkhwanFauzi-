-- Convert the H2 enum column created in milestone 1 so existing claims
-- can move into statuses introduced in this milestone.
ALTER TABLE IF EXISTS claims ALTER COLUMN status SET DATA TYPE VARCHAR(32);
