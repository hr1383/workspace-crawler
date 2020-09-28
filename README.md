# Dropwizard Scala Bootstrap
Minimal Example Scala Web Service using dropwizard-scala

### How to start the server
mvn clean install; java -jar server/target/vicaya-service-0.0.0-SNAPSHOT.jar server conf/workspace-service.yaml.erb

#### Endpoint Examples
curl -XPOST http://localhost:8080/v1/crawl/images/ -d 'http://www.bbc.com/' -H 'Content-Type: application/json' | python -m json.too

curl "http://localhost:8080/v1/search?text=curl"

curl -XPOST -v http://localhost:8080/user/create -d '{"userName": "rohit","accountSid": "vicaya"}' -H "Accept: application/json" -H "Content-Type: application/json"

curl -XGET http://localhost:8080/user/find/USe04103cdb8c528ea2b6f3a46a57b542d

curl -XPOST http://localhost:8080/v1/box/crawl -H 'Content-Type: application/json' -H "Accept: application/json"

curl -XPOST http://localhost:8080/v1/dropbox/crawl -H 'Content-Type: application/json' -H "Accept: application/json"

curl -XPOST http://localhost:8080/v1/github/crawl -H 'Content-Type: application/json' -H "Accept: application/json"


#### How to start in embedded postgres
On Mac: 

`brew install postgresql` 

`pgcli -U postgres`

`postgres@/tmp:postgres> \i vicaya.sql;`

`postgres@/tmp:postgres> \d`


#### To connect with GDoc API 
Create developer credentials 

console.developers.google.com/apis/credentials (give premission to read/download the files)

download the credentials files and update credentials.json in the project. 

When the search API is requested first time, the url will redirect to get the permission and after the permission is granted it will redirect back. After approved the tokens.json file is used to authenticate the request until the token expires. If token expires then system should be able to refresh the token or else have to perform the above steps again.
