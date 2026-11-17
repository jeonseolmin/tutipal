-- 물리 ERD 용 스키마 덤프: 테이블 | 컬럼 | 타입 | 길이 | null | PK | 참조 테이블 | UK (Flyway · Spring Session 테이블 제외)
select c.table_name, c.column_name, c.data_type, c.character_maximum_length, c.is_nullable,
 coalesce((select 'PK' from information_schema.table_constraints tc join information_schema.key_column_usage k on tc.constraint_name = k.constraint_name and tc.table_name = k.table_name
   where tc.constraint_type = 'PRIMARY KEY' and k.table_name = c.table_name and k.column_name = c.column_name limit 1), ''),
 coalesce((select ccu.table_name from information_schema.table_constraints tc join information_schema.key_column_usage k on tc.constraint_name = k.constraint_name and tc.table_name = k.table_name
   join information_schema.constraint_column_usage ccu on ccu.constraint_name = tc.constraint_name
   where tc.constraint_type = 'FOREIGN KEY' and k.table_name = c.table_name and k.column_name = c.column_name limit 1), ''),
 coalesce((select 'UK' from information_schema.table_constraints tc join information_schema.key_column_usage k on tc.constraint_name = k.constraint_name and tc.table_name = k.table_name
   where tc.constraint_type = 'UNIQUE' and k.table_name = c.table_name and k.column_name = c.column_name limit 1), '')
from information_schema.columns c join information_schema.tables t on t.table_name = c.table_name and t.table_schema = c.table_schema
where c.table_schema = 'public' and t.table_type = 'BASE TABLE' and c.table_name not like 'flyway%' and c.table_name not like 'spring_session%'
order by c.table_name, c.ordinal_position
