# Dropwizard Scala Bootstrap
Minimal Example Scala Web Service using dropwizard-scala

### How to start the server
mvn clean install; java -jar server/target/work-space-service-0.0.0-SNAPSHOT.jar server conf/workspace-service.yaml.erb

#### Endpoint Examples
curl -XPOST http://localhost:9460/v1/crawl/images/ -d 'http://www.bbc.com/' -H 'Content-Type: application/json' | python -m json.too
