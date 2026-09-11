# kb-spilsamlingen

**Description of the application goes here**

Developed and maintained by the Royal Danish Library.

## Requirements

* Maven 3                                  
* Java 17

## Setup

**PostgreSQL database creation, Solr installation etc. goes here**

## Build & run

Build with
``` 
mvn package
```

Test the webservice with
```
mvn jetty:run
```

The default port is 8081 and the default searchpage:
<http://http://localhost:8081/kb-spilsamlingen/search2.html/>

The Swagger UI is available at <http://localhost:8081/kb-spilsamlingen/api/>, providing access to both the `v1` and the 
`devel` versions of the GUI. 

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
