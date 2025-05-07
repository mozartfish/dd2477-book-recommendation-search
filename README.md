Set up correct version (9.0.0) of Elasticsearch and Kibana in Docker for local development (ONLY) by running the following script in the project directory
```
curl -fsSL https://elastic.co/start-local | sh -s -- -v 9.0.0
```
Other versions might also work but not guaranteed

Java 23 is required

In order for the java program to be able to connect to the Elasticsearch instance the credentials need to be placed in file `src/main/java/se/kth/booksearcher/data/BookEngine.java` on line 39

## How to run Webscraper
(Elasticsearch needs to be running)

Install the packages from requirements.txt

Input the credentials to the Elasticsearch instance at the top of the main.py file (line 13)

Run the script with `python3 main.py`

