# Lamp 
<strong> [c-Lightning](https://github.com/ElementsProject/lightning) Android Mobile Porting </strong>

[![Build Status](https://travis-ci.com/lvaccaro/lamp.svg?branch=master)](https://travis-ci.com/lvaccaro/lamp)

> This is a experimenting lightning wallet for testnet. It is only for development purpose, don't use on mainnet.

Touch the lamp to download and run c-lightning from cross-compilated binaries for Android are available [here]( https://github.com/lvaccaro/bitcoin_ndk/releases/tag/v0.18.1.2) or in alternative from source of
[bitcoin_ndk project](https://github.com/lvaccaro/bitcoin_ndk/tree/cln_test).

Warning: Lamp doesn't support Android >= 10.

![Spark screenshot](doc/img/Screen2.png)
![Spark screenshot](doc/img/Screen1.png)
![Spark screenshot](doc/img/Screen3.png)

#### Automatic Tor setup
Lamp is using tor hidden service as default. A new hidden service will be created at the first running time.

#### Manually Tor setup
Open [Orbot](https://github.com/guardianproject/Orbot) and setup a fixed tor address by menu: Onion Services -> Hosted Services -> set a service name and port 9735. Restarting tor to discover and copy the local address.

On Lamp settings, enable proxy using orbot localhost gateway:

- proxy: 127.0.0.1:9050
- announce address: tor_address
- bind address: 127.0.0.1:9735

Read the follow instruction at [Tor on clightning](https://lightning.readthedocs.io/TOR.html) to setup address on different network scenario.

#### References

- [ABCore](https://github.com/greenaddress/abcore) Android Bitcoin Core wallet
- [bitcoin_ndk](https://github.com/greenaddress/bitcoin_ndk) ndk build of bitcoin core and knots
- [clightning_ndk](https://github.com/lvaccaro/clightning_ndk) android cross-compilation of c-lightning for Android >= 24 Api
- [c-lightning](https://github.com/ElementsProject/lightning) a Lightning Network implementation in C
