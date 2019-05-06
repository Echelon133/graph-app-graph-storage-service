# graph-storage-service of graph-app

The crucial component of this service is a custom graph library that allows for:
* serialization/deserialization of graphs
* validation of graph correctness (edges cannot be negative, edges cannot reference vertexes that are not a part of the graph, etc.)
* limiting the number of edges a graph can have (so that servers cannot be attacked by making them calculate Dijkstra algorithm on millions of edges)

This service implements API that:
* makes it possible to receive, validate and save directed graphs
* allows the API client to read graphs from the database (graphs are identified by IDs)

This service "owns" the Redis graph database. Any other service that wants to read graphs
has to do it through this service (e.g. by a Feign client).



