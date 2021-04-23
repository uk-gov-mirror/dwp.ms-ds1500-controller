Feature: DS1500 controller for submissions and pdf download

  Scenario: Send a gp consultant body to the controller
    Given the httpClient is up
    And I create an sns topic named "ds1500-topic"
    And I create a catch all subscription for queue name "test-queue-1" binding to topic "ds1500-topic"
    When I hit the service url "http://localhost:9013/controller" with a post request gp consultant json body
    Then I get a http response of 200
    And I wait 2 seconds to guarantee message delivery
    And a message is successfully removed from the queue, there were a total of 1 messages on queue "test-queue-1"
    And the message has a correlation id and is a valid DSForm matching the following submission information
      | dateOfBirth         | "15/02/1972"                   |
      | diagnosisDate       | "05/2015"                      |
      | clinicalFeatures    | "Mr Coupe's clinical features" |
      | nINumber            | "NY921108A"                    |
      | representative3     | "NW1 3ER"                      |
      | declarerName        | "Dr. Michael Hfuhruhurr"       |
      | declarerPhoneNumber | "0114 258 8520"                |
      | other               | ""                             |
      | gmcNumber           | 1234567                        |

  Scenario: Send an other declarer body to the controller
    Given the httpClient is up
    And I create an sns topic named "ds1500-topic"
    And I create a catch all subscription for queue name "test-queue-2" binding to topic "ds1500-topic"
    When I hit the service url "http://localhost:9013/controller" with a post request 'other' declarer json body
    Then I get a http response of 200
    And I wait 2 seconds to guarantee message delivery
    And a message is successfully removed from the queue, there were a total of 1 messages on queue "test-queue-2"
    And the message has a correlation id and is a valid DSForm matching the following submission information
      | dateOfBirth         | "15/02/1972"                   |
      | diagnosisDate       | "05/2015"                      |
      | clinicalFeatures    | "Mr Coupe's clinical features" |
      | nINumber            | "NY921108A"                    |
      | representative3     | "NW1 3ER"                      |
      | declarerName        | "McMillan Nurse"               |
      | declarerPhoneNumber | "0114 258 8520"                |
      | other               | "Nurse"                        |
      | gmcNumber           | 0                              |

  Scenario: Send an bad declarer body to the controller
    Given the httpClient is up
    When I hit the service url "http://localhost:9013/controller" with a mismatched post declaration json body
    Then I get a http response of 400
    And the response contains no Json

  Scenario: Send a gp consultant body to the controller with a large json body
    Given the httpClient is up
    And I create an sns topic named "ds1500-topic"
    And I create a catch all subscription for queue name "test-queue-3" binding to topic "ds1500-topic"
    When I hit the service url "http://localhost:9013/controller" with a post request gp consultant json body
    Then I get a http response of 200
    And I wait 2 seconds to guarantee message delivery
    And a message is successfully removed from the queue, there were a total of 1 messages on queue "test-queue-3"
    And the message has a correlation id and is a valid DSForm matching the following submission information
      | dateOfBirth         | "15/02/1972"                   |
      | diagnosisDate       | "05/2015"                      |
      | clinicalFeatures    | "Mr Coupe's clinical features" |
      | nINumber            | "NY921108A"                    |
      | representative3     | "NW1 3ER"                      |
      | declarerName        | "Dr. Michael Hfuhruhurr"       |
      | declarerPhoneNumber | "0114 258 8520"                |
      | other               | ""                             |
      | gmcNumber           | 1234567                        |

  Scenario: Send an 'other' declarer body to the controller with a large json body
    Given the httpClient is up
    And I create an sns topic named "ds1500-topic"
    And I create a catch all subscription for queue name "test-queue-4" binding to topic "ds1500-topic"
    When I hit the service url "http://localhost:9013/controller" with a post request 'other' declarer json body
    Then I get a http response of 200
    And I wait 2 seconds to guarantee message delivery
    And a message is successfully removed from the queue, there were a total of 1 messages on queue "test-queue-4"
    And the message has a correlation id and is a valid DSForm matching the following submission information
      | dateOfBirth         | "15/02/1972"                   |
      | diagnosisDate       | "05/2015"                      |
      | clinicalFeatures    | "Mr Coupe's clinical features" |
      | nINumber            | "NY921108A"                    |
      | representative3     | "NW1 3ER"                      |
      | declarerName        | "McMillan Nurse"               |
      | declarerPhoneNumber | "0114 258 8520"                |
      | other               | "Nurse"                        |
      | gmcNumber           | 0                              |

  Scenario: Send a json body to the controller for download of the PDF file
    Given the httpClient is up
    And the PDF download service will return a response of 200 and a PDF file
    When I hit the service url "http://localhost:9013/download" with a json form post gp consultant json body
    Then I get a http response of 200
    And the response contains a PDF file
    And the response content type is "application/download"
    And the response contains no Json

  Scenario: Send a json body to the controller for fee download of the PDF file when it is not needed
    Given the httpClient is up
    And the PDF download service will return a response of 200 and a PDF file
    When I hit the service url "http://localhost:9013/downloadFee" with a json form post 'other' declarer json body
    Then I get a http response of 400
    And the response contains no Json

  Scenario: Send a json body to the controller for fee download of the PDF file and test it is created
    Given the httpClient is up
    And the PDF FEE download service will return a response of 200 and a PDF file
    When I hit the service url "http://localhost:9013/downloadFee" with a json form post gp consultant json body
    Then I get a http response of 200
    And the response contains no Json

  Scenario: Send an invalid json body to the controller
    Given the httpClient is up
    When I hit the service url "http://localhost:9013/controller" with an invalid body
    Then I get a http response of 400
    And the response contains no Json

  Scenario: Pdf Generator service is unavailable causing the service to response with 500 when requesting download
    Given the httpClient is up
    And the PDF download service is unavailable
    When I hit the service url "http://localhost:9013/download" with a json form post gp consultant json body
    Then I get a http response of 500
    And the response contains no Json
