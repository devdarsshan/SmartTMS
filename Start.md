Order Service:

1. An order should have id, createdAt, DeliveredAt, from, to, status(Created, Assigned, In_Transist, Delivered), vehicle id.
2. On Create order method validate if The new order created by the user with role DISPATCHER
3. An order can be assigned to vehicle that's currently idle and it's city belongs to the order's from city.The order can be assigned to the vehicle with statis idle or occupied.
4. and the order's status can be updated to "Assigned" once the vehicle is found and updated.
5. When the order is Deliverd and it's status updates, then orderid should be removed from the vehicle's order list.

Vehicle-service

1. An API to update a vehicle to Intransit state also revert it back to idle state
2. Maintain a list of orders assigned to the vehicle,and After assigning an order to the vehicle, it should become occupied if it's idle
3. If no orders in the list, then it becomes idle automatically
4. When fetching based on status and city, I want the api(existing) to filter both idle and occupied vehicles also to be returned as it still can take order
5. Update "City" to "From" and introduce new fields like "TO" and "Through", where through will be a list of string.
6. While creating a vehicle all from, to, through should be mandatory
7. Add or update the api, that allows to fetch vehiles based on the from,to of an order that's in idle or occupied state, if the to is in the through points also, that also should be returned in the api