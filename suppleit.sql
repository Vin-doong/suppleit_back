CREATE TABLE Member (
  member_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '회원 고유 ID',
  email VARCHAR(200) NOT NULL UNIQUE COMMENT '로컬/소셜 로그인 이메일',
  password VARCHAR(200) NULL COMMENT '로컬 로그인은 필수, 소셜 로그인 사용자는 NULL',
  nickname VARCHAR(50) NULL COMMENT '닉네임',
  gender VARCHAR(10) NULL COMMENT '성별 (MALE/FEMALE, 소셜 로그인에 따라 NULL 허용)',
  birth DATE NULL COMMENT '생년월일 (소셜 로그인에 따라 NULL 허용)',
  member_role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '사용자 역할',
  social_type VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '소셜 로그인 유형',
  CONSTRAINT chk_member_role CHECK (member_role IN ('USER', 'ADMIN')),
  CONSTRAINT chk_social_type CHECK (social_type IN ('NONE', 'KAKAO', 'NAVER', 'GOOGLE'))
);

CREATE TABLE `Product` (
	`prd_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`product_name` VARCHAR(50) NOT NULL,
	`company_name` VARCHAR(50) NOT NULL,
	`registration_no` VARCHAR(200) NULL,
	`expiration_period` VARCHAR(50) NULL,
	`srv_use` TEXT NULL,
	`main_function` TEXT NULL,
	`preservation` TEXT NULL,
	`intake_hint` TEXT NULL,
	`base_standard` TEXT NULL
);

CREATE TABLE `Favorite` (
	`favorite_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`member_id` BIGINT NOT NULL,
	`prd_id` BIGINT NOT NULL,
	FOREIGN KEY (`member_id`) REFERENCES `Member`(`member_id`) ON DELETE CASCADE,
	FOREIGN KEY (`prd_id`) REFERENCES `Product`(`prd_id`) ON DELETE CASCADE,
	UNIQUE KEY (`member_id`, `prd_id`) -- ✅ 같은 제품 중복 즐겨찾기 방지
);

CREATE TABLE `Schedule` (
	`schedule_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`intake_start` DATE NOT NULL,
	`intake_distance` INT NULL, -- ✅ INT로 변경 가능
	`intake_end` DATE NULL,
	`memo` TEXT NULL,
	`member_id` BIGINT NOT NULL,
	`prd_id` BIGINT NOT NULL,
	FOREIGN KEY (`member_id`) REFERENCES `Member`(`member_id`) ON DELETE CASCADE,
	FOREIGN KEY (`prd_id`) REFERENCES `Product`(`prd_id`) ON DELETE CASCADE
);

CREATE TABLE `Notice` (
	`notice_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`title` VARCHAR(200) NOT NULL,
	`content` TEXT NOT NULL,
	`member_id` BIGINT NOT NULL,
	FOREIGN KEY (`member_id`) REFERENCES `Member`(`member_id`) ON DELETE CASCADE
);

CREATE TABLE `Review` (
	`review_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`rating` VARCHAR(20) NULL, -- ✅ ENUM → VARCHAR 변경
	`content` TEXT NULL,
	`image` VARCHAR(200) NULL,
	`member_id` BIGINT NOT NULL,
	`prd_id` BIGINT NOT NULL,
	FOREIGN KEY (`member_id`) REFERENCES `Member`(`member_id`) ON DELETE CASCADE,
	FOREIGN KEY (`prd_id`) REFERENCES `Product`(`prd_id`) ON DELETE CASCADE
);
