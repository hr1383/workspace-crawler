# Dropwizard Scala Bootstrap
Minimal Example Scala Web Service using dropwizard-scala

### How to start the server
mvn clean install; java -jar server/target/vicaya-service-0.0.0-SNAPSHOT.jar server conf/workspace-service.yaml.erb

#### Endpoint Examples
curl -XPOST http://localhost:9090/v1/crawl/images/ -d 'http://www.bbc.com/' -H 'Content-Type: application/json' | python -m json.too

curl "http://localhost:9090/v1/search?text=curl"

curl -XPOST -v http://localhost:9090/user/create -d '{"userName": "rohit","accountSid": "vicaya"}' -H "Accept: application/json" -H "Content-Type: application/json"

curl -XGET http://localhost:9090/user/find/USe04103cdb8c528ea2b6f3a46a57b542d

curl -XPOST http://localhost:9090/v1/box/crawl -H 'Content-Type: application/json' -H "Accept: application/json"

curl -XPOST http://localhost:9090/v1/dropbox/crawl -H 'Content-Type: application/json' -H "Accept: application/json"

curl -XPOST http://localhost:9090/v1/github/crawl -H 'Content-Type: application/json' -H "Accept: application/json"


#### How to start in embedded postgres
On Mac: 

`brew install postgresql`

`brew services start postgres`
`sudo -u <username> createuser --superuser postgres`

`sudo -u rgupta  createdb postgres`

`touch .psql_history`

`psql -U postgres -W`

`postgres# create database vicayah;`

`postgres# create user postgres with encrypted password 'iAMs00perSecrEET';`

`postgres=# grant all privileges on database vicayah to postgres;`

`postgres=#  \i /Users/<username>/Development/vicaya/workspace-crawler/conf/db/V1_Initial_Schema_Create.sql;`

`postgres=# \c vicayah;

 Password for user postgres:

 You are now connected to database "vicayah" as user "postgres".`
`postgres@/tmp:postgres> \d`

# Install Zookeeper
brew install zookeeper
brew services start zookeeper

# Install Kafka
brew install kafka
brew services start kafka
kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic file-metadata

#### To connect with GDoc API 
Create developer credentials 

console.developers.google.com/apis/credentials (give premission to read/download the files)

download the credentials files and update credentials.json in the project. 

When the search API is requested first time, the url will redirect to get the permission and after the permission is granted it will redirect back. After approved the tokens.json file is used to authenticate the request until the token expires. If token expires then system should be able to refresh the token or else have to perform the above steps again.
