create table if not exists notes (
    id         serial primary key,
    text       varchar(500) not null,
    created_at timestamp default now()
);
