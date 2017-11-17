# DSI Monitoring CLI

CLI to monitor DSI servers by using the IBM JDK JMX Beans.

## Compilation

Copy from the DSI distribution the restConnector.jar to the directory lib, then
compile the maven project:

`mvn clean install`

## Run

`target/dsi-monitoring.sh --username tester --password tester <hostname:port> <hostname2:port2> ... -i <interval>
`

will display the memory usage each `<interval>` seconds.

## Usage

```
Usage: <main class> [options] server jmx urls
Options:
  -a, --all

     Default: false
  -d, --directory

  -h, --help

     Default: false
  -i, --interval

     Default: 0
  -p, --password

     Default: tester
  -s, --shutdown

     Default: false
  -u, --username

     Default: tester
```       

## Example of usage

To display each 30s the memory usage of 2 DSI servers:

`target/dsi-monitoring.sh server1_hostname:port server2_hostname:port -i 30`

To do something when the number of direct buffer is greater than a given number:

```
#!/bin/bash

count=`target/dsi-monitoring.sh localhost:9999 | grep "#Count" | cut -d "," -f 4`

if [[ "$count" > "9999" ]];
then
        # do something when the number of direct buffers if greater than 9999
fi
```

To display all MBeans:
`target/dsi-monitoring.sh hostname:port --all`


# Issues and contributions
For issues relating specifically to the Dockerfiles and scripts, please use the [GitHub issue tracker](../../issues).
We welcome contributions following [our guidelines](CONTRIBUTING.md).

# License
The Dockerfiles and associated scripts found in this project are licensed under the [Apache License 2.0](LICENSE).

# Notice
© Copyright IBM Corporation 2017.
