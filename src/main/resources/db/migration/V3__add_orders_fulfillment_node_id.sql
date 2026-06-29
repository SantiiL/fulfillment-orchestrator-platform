alter table orders
    add column fulfillment_node_id uuid;

alter table orders
    add constraint fk_orders_fulfillment_node
        foreign key (fulfillment_node_id) references fulfillment_nodes (id);
