create table orders (
    id uuid primary key,
    seller_id uuid not null,
    status varchar(50) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
