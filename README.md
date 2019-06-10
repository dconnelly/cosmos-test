## Running Tests

Run tests with existing database:

    $ mvn test -DcosmosEndpoint=<endpoint> -DcosmosKey=<key>

Reset database before running tests:

    $ mvn test -DcosmosEndpoint=<endpoint> -DcosmosKey=<key> -DresetDatabase

Run just the ```COUNT(1)``` query test:

    $ mvn test -DcosmosEndpoint=<endpoint> -DcosmosKey=<key> -Dtest=QueryTest#testCount


