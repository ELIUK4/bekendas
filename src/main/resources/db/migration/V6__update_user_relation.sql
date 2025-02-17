-- Atnaujinti user_id ryšį
UPDATE image i
SET user_id = (SELECT id FROM user_entity u WHERE u.id = i.user_id)
WHERE user_id IS NOT NULL;

-- Pridėti foreign key constraint
ALTER TABLE image
ADD CONSTRAINT fk_image_user_entity
FOREIGN KEY (user_id)
REFERENCES user_entity(id);
