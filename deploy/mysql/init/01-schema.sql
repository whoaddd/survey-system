CREATE DATABASE IF NOT EXISTS user_db     DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS survey_db   DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS response_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE user_db;     -- 建 user



CREATE TABLE `user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`      VARCHAR(64)  NOT NULL                COMMENT '登录名',
  `password_hash` VARCHAR(100) NOT NULL                COMMENT 'BCrypt 哈希',
  `role`          VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHER' COMMENT 'PUBLISHER/ADMIN',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '1启用 0禁用',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发卷者账号';


USE survey_db;   -- 建 survey / question / question_option
CREATE TABLE `survey` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `creator_id`        BIGINT       NOT NULL              COMMENT '发卷者 user.id',
  `title`             VARCHAR(200) NOT NULL,
  `description`       TEXT         NULL,
  `status`            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/CLOSED',
  `share_code`        VARCHAR(12)  NULL                  COMMENT '发布时生成的 8 位 base62',
  `version`           INT          NOT NULL DEFAULT 1    COMMENT '当前草稿版本',
  `published_version` INT          NULL                  COMMENT '已发布的冻结版本',
  `published_at`      DATETIME     NULL,
  `closed_at`         DATETIME     NULL,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_code` (`share_code`),
  KEY `idx_creator_status` (`creator_id`, `status`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问卷';

CREATE TABLE `question` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `survey_id`     BIGINT       NOT NULL,
  `version`       INT          NOT NULL              COMMENT '所属问卷版本',
  `type`          VARCHAR(20)  NOT NULL              COMMENT 'TEXT/IMAGE/SINGLE/MULTI/RATING',
  `title`         VARCHAR(500) NOT NULL              COMMENT '题干纯文本',
  `content_html`  TEXT         NULL                  COMMENT '图文混排 HTML（后端净化后存储）',
  `image_key`     VARCHAR(255) NULL                  COMMENT '图片题附件 object_key',
  `required`      TINYINT      NOT NULL DEFAULT 0,
  `score_enabled` TINYINT      NOT NULL DEFAULT 0    COMMENT '是否作为评分题',
  `score_min`     INT          NOT NULL DEFAULT 1,
  `score_max`     INT          NOT NULL DEFAULT 5,
  `sort_order`    INT          NOT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_survey_version` (`survey_id`, `version`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目';

CREATE TABLE `question_option` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT       NOT NULL,
  `label`       VARCHAR(200) NOT NULL  COMMENT '展示文案',
  `value`       VARCHAR(100) NOT NULL  COMMENT '存储值',
  `sort_order`  INT          NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_question` (`question_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目选项';


USE response_db; -- 建 response / response_answer / attachment_ref / ai_evaluation
CREATE TABLE `response` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT,
  `survey_id`       BIGINT      NOT NULL,
  `survey_version`  INT         NOT NULL              COMMENT '提交时问卷的 published_version',
  `respondent_key`  VARCHAR(64) NOT NULL              COMMENT '前端 UUID，防重标识',
  `receipt_code`    VARCHAR(16) NOT NULL              COMMENT '提交回执',
  `ip_hash`         VARCHAR(64) NULL                  COMMENT 'IP 哈希，用于防刷审计',
  `user_agent`      VARCHAR(255) NULL,
  `submitted_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_survey_respondent` (`survey_id`, `respondent_key`),
  UNIQUE KEY `uk_receipt_code` (`receipt_code`),
  KEY `idx_survey_time` (`survey_id`, `submitted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='答卷';

CREATE TABLE `response_answer` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
  `response_id`         BIGINT       NOT NULL,
  `question_id`         BIGINT       NOT NULL,
  `answer_text`         TEXT         NULL     COMMENT '文本题答案',
  `selected_option_ids` JSON         NULL     COMMENT '单选/多选选项 id 数组',
  `rating_value`        INT          NULL     COMMENT '评分题分值',
  `attachment_key`      VARCHAR(255) NULL     COMMENT '图片题答案 object_key',
  `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_response_question` (`response_id`, `question_id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='答卷答案';

CREATE TABLE `attachment_ref` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_type`    VARCHAR(20)  NOT NULL COMMENT 'SURVEY/QUESTION/RESPONSE',
  `biz_id`      BIGINT       NULL COMMENT '对应业务主键，上传时可为空',,
  `object_key`  VARCHAR(255) NOT NULL COMMENT 'MinIO 对象键',
  `mime_type`   VARCHAR(100) NOT NULL,
  `size`        BIGINT       NOT NULL,
  `uploader_id` BIGINT       NULL     COMMENT '发卷者 id；答卷附件为 NULL',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_object_key` (`object_key`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='附件元数据';

CREATE TABLE `ai_evaluation` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `response_id`    BIGINT       NOT NULL,
  `question_id`    BIGINT       NOT NULL,
  `model_name`     VARCHAR(100) NOT NULL COMMENT '如 gpt-4o-mini',
  `prompt_version` VARCHAR(20)  NOT NULL COMMENT 'Prompt 模板版本',
  `rating`         INT          NULL     COMMENT 'AI 评分 1-5',
  `reason`         TEXT         NULL,
  `suggestion`     TEXT         NULL,
  `confidence`     DECIMAL(3,2) NULL,
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED/PENDING_REVIEW',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_response_question_model` (`response_id`, `question_id`, `model_name`),
  KEY `idx_response` (`response_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 评分审计';