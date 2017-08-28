# free-home-Bridge
Free@home is using XMPP for homeautomatisation this bridge trys to simplyfy the communication between systems like openhab and other open source systems

Curently it is Testet with a busch jaeger free@home SYSAP it should also run with a ABB labed system.



## Build

To build the Serve maven and a jdk 1.8 is required. 
Just Clone and run maven

```

    git clone https://github.com/hasisunkid/free-home-Bridge.git
    cd free-home-bridge
    mvn install

```

## Configuration

there is a template configuration file `config.cfg` to adapt it to your environment pleas edit it.


## Installation
Just copy the jar file and the /lib directory to your prevered location.

## Run

```

    java -Xmx20m -jar FreeHomeBridge-0.1-SNAPSHOT.jar config.cfg

```


