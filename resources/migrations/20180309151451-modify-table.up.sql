alter table users drop column pass;
--;;
alter table users drop column last_login;
--;;
alter table users drop column admin;
--;;

alter table users add create_time timestamp;
--;;
alter table users add address varchar(255);
--;;
alter table users add sex boolean;
--;;
alter table users add phone varchar(50);
