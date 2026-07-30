create table fulfillment_node_working_days (
    fulfillment_node_id uuid not null,
    day_of_week varchar not null,
    primary key (fulfillment_node_id, day_of_week),
    constraint fk_fulfillment_node_working_days_fulfillment_node
        foreign key (fulfillment_node_id) references fulfillment_nodes (id)
);

insert into fulfillment_node_working_days (fulfillment_node_id, day_of_week)
select fulfillment_node.id, working_day.day_of_week
from fulfillment_nodes fulfillment_node
cross join (
    values
        ('MONDAY'),
        ('TUESDAY'),
        ('WEDNESDAY'),
        ('THURSDAY'),
        ('FRIDAY'),
        ('SATURDAY'),
        ('SUNDAY')
) as working_day(day_of_week);
