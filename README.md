## Running Tests

To run tests with existing database:

    $ mvn test -DcosmosEndpoint=<endpoint> -DcosmosKey=<key>

To reset database before running tests:

    $ mvn test -DcosmosEndpoint=<endpoint> -DcosmosKey=<key> -DresetDatabase


