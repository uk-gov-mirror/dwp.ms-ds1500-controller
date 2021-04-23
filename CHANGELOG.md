<a name="1.6.0"></a>
# 1.6.0 (2021-04-22)

Add in field validations ([c91600e3](https://gitlab.nonprod.dwpcloud.uk/health/ds1500/components/ms-ds1500-controller/commit/c91600e3/)) 

<a name="1.5.0"></a>
# 1.5.0 (2021-02-16)

Add null defaults to ssl env vars ([ed4fd351](https://gitlab.nonprod.dwpcloud.uk/health/ds1500/components/ms-ds1500-controller/commit/ed4fd351/)) 

<a name="1.3.0"></a>
# 1.3.0 (2021-01-18)

### Bug Fixes

* **versions:** Update jackson-databind ([e4f912403](https://gitlab.nonprod.dwpcloud.uk/health/ds1500/components/ms-ds1500-controller/commit/e4f912403/))

<a name="1.2.0"></a>
# 1.2.0 (2019-09-13)

### Bug Fixes

* **versions:** update messaging-utility version to v2.0.1 and dependency suppression for databind ([2fcff2a](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/2fcff2a))


<a name="1.0.0"></a>
# 1.0.0 (2019-04-30)


### Bug Fixes

* **owasp:** fix jetty vulnerability ([04b81f0](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/04b81f0))


### Features

* **config:** update configuration to support sns and add dependencies and remove rabbit items ([c29b341](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/c29b341))
* **ECR:** fixing component name in line with ECR destination naming convention ([2cdcbc2](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/2cdcbc2))
* **localstack:** implement chabnges to support localstack and SNS ([51954fd](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/51954fd))
* **rabbit:** removing RabbitMQ references for tests ([845ec7f](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/845ec7f))
* **sns:** add in MessagePublisher class for SNS ([b3ec592](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/b3ec592))



<a name="0.12.0"></a>
# 0.12.0 (2019-01-24)


### Bug Fixes

* **diagnosis:** fixed diagnosis date error when in the current month ([8c147c8](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/8c147c8))
* **owasp:** fixed owasp vulnerability ([f1d73ea](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/f1d73ea))



<a name="0.11.0"></a>
# 0.11.0 (2018-10-31)



<a name="0.10.0"></a>
# 0.10.0 (2018-10-16)



<a name="0.9.0"></a>
# 0.9.0 (2018-07-20)


### Bug Fixes

* **bug:** found bug when presenting and empty string as the representative postcode.  an empty postcode is ok for the this value.  added new test ([92a7ef5](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/92a7ef5))
* **deps:** initial dependency updates and sonar formatting ([fcdef43](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/fcdef43))
* **import:** removed invalid (and unused) import ([38b901a](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/38b901a))
* **nino:** remove NINO class and include NinoValidator and PostcodeValidator to verify content with associated test changes ([8b884c1](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/8b884c1))
* **packages:** move to uk.gov... package naming convention ([920cb4c](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/920cb4c))
* **postcode:** remove PostCode from the service, not needed anymore ([d61a308](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/d61a308))
* **ssl:** no longer need ssl configuration entries for drs communicator ([f89ed14](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/f89ed14))


### Features

* **build:** adding build files and changing the name of the service to match HealthPDU standards ([1be2051](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/1be2051))
* **logger:** update DwpEncodedLogger with LoggerFactory for log binding ([a30d1e9](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/a30d1e9))
* **versions:** updated dwp dependency version and dropwizard, update configuration and suppress bootstrap logger for dropwiz ([07b515c](https://gitlab.nonprod.dwpcloud.uk/SecureComms/ds1500-controller/commit/07b515c))



