<a name="1-11-0"></a>
# 1-11-0 (2024-02-13)

* htf 2905: remove repo gen ([ca08d3a](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/-/commit/ca08d3a))
* feature: update document ID ([1a882e5](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/-/commit/1a882e5))
* feature: add new postcode field ([13443a6](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/-/commit/13443a6))
* feature: date of diagnosis ([41b10e1](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/-/commit/41b10e1))

<a name="1-10-1"></a>
# 1-10-1 (2023-10-05)

* chore: update vulnerabilities ([905ea40](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/-/commit/905ea40))
* chore: add project metadata [ci skip] ([916d146](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/-/commit/916d146))
* chore: update to java 17 ([29bbde9](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/-/commit/29bbde9))
* chore: update dependency junit to v4.13.1 ([f8bae27](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/-/commit/f8bae27))

<a name="1.10.0"></a>
# 1.10.0 (2023-04-03)

* **srel policy changes:** update ds1500 to sr1 ([666d6a60](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/commit/666d6a60/))
* **versions:** Upgrade message utility versions ([531cfcb3](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/commit/531cfcb3/))

<a name="1.9.0"></a>
# 1.9.0 (2023-02-23)

**docker:** update dockerfile user ([2019ec5c](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/commit/2019ec5c/))

<a name="1.7.2"></a>
# 1.7.2 (2022-01-20)

**vulnerability:** update log4j to 2.17.1 ([389b0d8a](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/commit/389b0d8a/))


<a name="1.7.1"></a>
# 1.7.1 (2021-12-23)

**vulnerability:** update log4j to 2.17.0 ([9d38b0e](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/commit/9d38b0e/))
**vulnerability:** update dropwizard to 2.0.28 ([d7697ca](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/commit/d7697ca/)) 

<a name="1.7.0"></a>
# 1.7.0 (2021-07-02)

**feat:** open sourcing ([8072cc3f](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/commit/8072cc3f/)) 

<a name="1.6.0"></a>
# 1.6.0 (2021-04-22)

Add in field validations ([c91600e3](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/commit/c91600e3/)) 

<a name="1.5.0"></a>
# 1.5.0 (2021-02-16)

Add null defaults to ssl env vars ([ed4fd351](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/commit/ed4fd351/)) 

<a name="1.3.0"></a>
# 1.3.0 (2021-01-18)

### Bug Fixes

* **versions:** Update jackson-databind ([e4f912403](https://gitlab.com/dwp/health/ds1500/components/ms-ds1500-controller/commit/e4f912403/))

<a name="1.2.0"></a>
# 1.2.0 (2019-09-13)

### Bug Fixes

* **versions:** update messaging-utility version to v2.0.1 and dependency suppression for databind ([2fcff2a](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/2fcff2a))


<a name="1.0.0"></a>
# 1.0.0 (2019-04-30)


### Bug Fixes

* **owasp:** fix jetty vulnerability ([04b81f0](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/04b81f0))


### Features

* **config:** update configuration to support sns and add dependencies and remove rabbit items ([c29b341](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/c29b341))
* **ECR:** fixing component name in line with ECR destination naming convention ([2cdcbc2](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/2cdcbc2))
* **localstack:** implement chabnges to support localstack and SNS ([51954fd](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/51954fd))
* **rabbit:** removing RabbitMQ references for tests ([845ec7f](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/845ec7f))
* **sns:** add in MessagePublisher class for SNS ([b3ec592](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/b3ec592))



<a name="0.12.0"></a>
# 0.12.0 (2019-01-24)


### Bug Fixes

* **diagnosis:** fixed diagnosis date error when in the current month ([8c147c8](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/8c147c8))
* **owasp:** fixed owasp vulnerability ([f1d73ea](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/f1d73ea))



<a name="0.11.0"></a>
# 0.11.0 (2018-10-31)



<a name="0.10.0"></a>
# 0.10.0 (2018-10-16)



<a name="0.9.0"></a>
# 0.9.0 (2018-07-20)


### Bug Fixes

* **bug:** found bug when presenting and empty string as the representative postcode.  an empty postcode is ok for the this value.  added new test ([92a7ef5](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/92a7ef5))
* **deps:** initial dependency updates and sonar formatting ([fcdef43](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/fcdef43))
* **import:** removed invalid (and unused) import ([38b901a](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/38b901a))
* **nino:** remove NINO class and include NinoValidator and PostcodeValidator to verify content with associated test changes ([8b884c1](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/8b884c1))
* **packages:** move to uk.gov... package naming convention ([920cb4c](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/920cb4c))
* **postcode:** remove PostCode from the service, not needed anymore ([d61a308](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/d61a308))
* **ssl:** no longer need ssl configuration entries for drs communicator ([f89ed14](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/f89ed14))


### Features

* **build:** adding build files and changing the name of the service to match HealthPDU standards ([1be2051](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/1be2051))
* **logger:** update DwpEncodedLogger with LoggerFactory for log binding ([a30d1e9](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/a30d1e9))
* **versions:** updated dwp dependency version and dropwizard, update configuration and suppress bootstrap logger for dropwiz ([07b515c](https://gitlab.com/dwp/SecureComms/ds1500-controller/commit/07b515c))
