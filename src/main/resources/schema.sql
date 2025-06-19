-- 기존 테이블 삭제
DROP TABLE IF EXISTS PERSON;


CREATE TABLE PERSON (
                                   ID BIGINT NOT NULL PRIMARY KEY,
                                   NAME VARCHAR(255),
                                   DATE VARCHAR(255)
);