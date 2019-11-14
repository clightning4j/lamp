# Lamp 
<strong> [c-Lightning](https://github.com/ElementsProject/lightning) Android Mobile Porting </strong>

[![Build Status](https://travis-ci.org/lvaccaro/Lamp.svg?branch=master)]

> This is a experimenting lightning wallet for testnet. It is only for development purpose, don't use on mainnet.

The c-lightning cross-compilated binaries for Android are available at [clightning_ndk project](https://github.com/lvaccaro/clightning_ndk).

#### Setup tor
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
