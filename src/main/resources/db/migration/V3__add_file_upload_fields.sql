ALTER TABLE images
ADD COLUMN file_name VARCHAR(255),
ADD COLUMN original_file_name VARCHAR(255),
ADD COLUMN upload_date TIMESTAMP;
