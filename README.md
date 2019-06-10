## Running Tests

To run tests with existing database:

    $ mvn test -DcosmosEndpoint=<endpoint> -DcosmosKey=<key>

To delete database before running tests:

    $ mvn test -DcosmosEndpoint=<endpoint> -DcosmosKey=<key> -Dclean=true


