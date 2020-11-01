package com.lvaccaro.lamp.utils

import org.json.JSONArray
import org.json.JSONObject

object SimulatorPlugin {

    fun onchain(jsonObject: JSONObject): Int {
        var onChainFunds = 0
        if (jsonObject.has("outputs")) {
            val outputs: JSONArray = jsonObject["outputs"] as JSONArray
            for (i in 0 until outputs.length()) {
                val output = outputs.getJSONObject(i)
                onChainFunds += output["value"] as Int
            }
        }
        return onChainFunds
    }

    fun offchain(jsonObject: JSONObject): Int {
        var offChainFunds = 0
        if (jsonObject.has("channels")) {
            val channels: JSONArray = jsonObject["channels"] as JSONArray
            for (i in 0 until channels.length()) {
                val channel = channels.getJSONObject(i)
                offChainFunds = channel["channel_sat"] as Int
            }
        }
        return offChainFunds
    }

    fun funds(listFunds: JSONObject): Int {
        var fundsToUs = 0
        val peers = listFunds["peers"] as JSONArray
        for (i in 0 until peers.length()) {
            val peer = peers.get(i) as? JSONObject
            val peerChannels = peer?.get("channels") as JSONArray
            for (j in 0 until peerChannels.length()) {
                val channel = peerChannels.get(j) as JSONObject
                fundsToUs += channel["msatoshi_to_us"] as Int
            }
        }
        return fundsToUs
    }
}