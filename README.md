# Lamp 
<strong> [c-Lightning](https://github.com/ElementsProject/lightning) Android Mobile Porting </strong>

[![Build Status](https://travis-ci.com/lvaccaro/lamp.svg?branch=master)](https://travis-ci.com/lvaccaro/lamp)
[![MIT license](https://img.shields.io/github/license/lvaccaro/lamp)](https://github.com/lvaccaro/lamp/blob/master/LICENSE)


> This is a experimenting lightning wallet for testnet. It is only for development purpose, don't use on mainnet.

Touch the lamp to download and run c-lightning from cross-compilated binaries for Android are available [here]( https://github.com/lvaccaro/lightning_ndk/releases/tag/release_clightning_0.8.1_1dc281c) or in alternative from source of
[bitcoin_ndk project](https://github.com/lvaccaro/lightning_ndk/tree/lightning).

![screenshot1](doc/img/Screen2.png)
![screenshot2](doc/img/Screen1.png)
![screenshot3](doc/img/Screen3.png)


## Bitcoin Setup

#### Automatic with esplora plugin

Lamp is using [esplora plugin](https://github.com/lvaccaro/esplora_clnd_plugin) to fetch chain/block/tx info and send tx from [blockstream.info](https://blockstream.info) explorer. Esplora plugin is enabled by default.


#### Manually with bitcoind rpc node
On Lamp settings, disable esplora plugin and set the current bitcoin rpc options:

- Bitcoin RPC username
- Bitcoin RPC password
- Bitcoin RPC host (default 127.0.0.1)
- Bitcoin RPC port (default 18332)

## Tor Setup

#### Automatic with internal tor service

Lamp is using tor hidden service as default. A new hidden service will be created at the first running time.

#### Manually with Orbot

Open [Orbot](https://github.com/guardianproject/Orbot) and setup a fixed tor address by menu: Onion Services -> Hosted Services -> set a service name and port 9735. Restarting tor to discover and copy the local address.

On Lamp settings, enable proxy using orbot localhost gateway:

- proxy: 127.0.0.1:9050
- announce address: tor_address
- bind address: 127.0.0.1:9735

Read the follow instruction at [Tor on clightning](https://lightning.readthedocs.io/TOR.html) to setup address on different network scenario.

## References

- [ABCore](https://github.com/greenaddress/abcore) Android Bitcoin Core wallet
- [bitcoin_ndk](https://github.com/greenaddress/bitcoin_ndk) ndk build of bitcoin core and knots
- [clightning_ndk](https://github.com/lvaccaro/clightning_ndk) android cross-compilation of c-lightning for Android >= 24 Api
- [c-lightning](https://github.com/ElementsProject/lightning) Lightning Network implementation in C
- [esplora plugin](https://github.com/lvaccaro/esplora_clnd_plugin) C-Lightning plugin for esplora
