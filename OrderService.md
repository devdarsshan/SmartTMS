1. An order should have id, createdAt, DeliveredAt, from, to, status(Created, Assigned, In_Transist, Delivered), vehicle id.
2. On Create order method validate if The new order created by the user with role DISPATCHER
3. An order can be assigned to vehicle that's currently idle and it's city belongs to the order's from city.The order can be assigned to the vehicle with statis idle or occupied.
4. and the order's status can be updated to "Assigned" once the vehicle is found and updated.
5. When the order is Deliverd and it's status updates, then orderid should be removed from the vehicle's order list.