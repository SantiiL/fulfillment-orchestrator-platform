create table fulfillment_nodes (
    id uuid primary key,
    code varchar(100) unique not null,
    name varchar(255) not null,
    max_daily_capacity integer not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
