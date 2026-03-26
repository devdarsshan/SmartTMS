1. An API to update a vehicle to Intransit state also revert it back to idle state
2. Maintain a list of orders assigned to the vehicle,and After assigning an order to the vehicle, it should become occupied if it's idle
3. If no orders in the list, then it becomes idle automatically
4. When fetching based on status and city, I want the api(existing) to filter both idle and occupied vehicles also to be returned as it still can take order
5. Update "City" to "From" and introduce new fields like "TO" and "Through", where through will be a list of string.
6. While creating a vehicle all from, to, through should be mandatory
7. Add or update the api, that allows to fetch vehiles based on the from,to of an order that's in idle or occupied state, if the to is in the through points also, that also should be returned in the api