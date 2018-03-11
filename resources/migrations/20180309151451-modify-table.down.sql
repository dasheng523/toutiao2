alter table users drop column create_time;
--;;
alter table users drop column address;
--;;
alter table users drop column sex;
--;;
alter table users drop column phone;
--;;

alter table users add pass varchar(300);
--;;
alter table users add last_login timestamp;
--;;
alter table users add admin boolean;

