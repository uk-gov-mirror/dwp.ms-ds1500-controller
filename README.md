# Ds1500-controller

RESTful Webservice taking JSON posted information for the ds1500 form.

Managed by the Dwp Health PDU.

## Table of contents

* Build
* Test
* Endpoints
* Health check
* Schedules
* Production Release

## Build

Using Java 11 and Dropwizard

* clone repository and run `mvn clean package`
* starting the service - `java -jar target/ms-ds1500-controller-<version>.jar server path/to/config.yml`

## Test

Using Java 11, Dropwizard, Junit 4

* mvn clean test

## Endpoints

**`/controller` *[POST]***

Main entry point to create and send (to DRS) the rendered pdf from the incoming json payload

**`/download` *[POST]***

Request to download the ds1500 generated pdf

**`/downloadFee` *[POST]***

Request to download the ds1500 fee generated pdf

## Healthcheck

Health check can be found at **`/healthcheck` *[GET]***

## Version-info (Enabled via the APPLICATION_INFO_ENABLED env var)

Version info can be found at **`/version-info` *[GET]***

### Schedules

The CI pipeline has a stage which sets up a schedule to run the `develop` branch every night 
- the schedule can be found in the `CI/CD/Schedules` section of Gitlab.

## Production Release

To create production artefacts the following process must be followed https://confluence.service.dwpcloud.uk/display/DHWA/SRE
