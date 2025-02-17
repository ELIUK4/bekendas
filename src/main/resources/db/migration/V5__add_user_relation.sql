-- Pašaliname seną user_id stulpelį
ALTER TABLE image DROP COLUMN IF EXISTS user_id;

-- Pridedame naują user_id stulpelį su foreign key
ALTER TABLE image ADD COLUMN user_id BIGINT;
ALTER TABLE image ADD CONSTRAINT fk_image_user FOREIGN KEY (user_id) REFERENCES user_entity(id);
