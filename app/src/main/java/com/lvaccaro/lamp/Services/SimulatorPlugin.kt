package com.lvaccaro.lamp.Services

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class SimulatorPlugin{

    companion object{

        val TAG = SimulatorPlugin::class.java.canonicalName

        fun funds(jsonObject: JSONObject): JSONObject{
            val response = JSONObject()
            var onChainFunds = 0;
            var offChainFunds = 0;
            if(jsonObject.has("outputs")){
                val outputs: JSONArray = jsonObject["outputs"] as JSONArray
                for (i in 0 until outputs.length()){
                    val output = outputs.getJSONObject(i)
                    onChainFunds += output["value"] as Int
                    Log.d(TAG, "****** On chain fund ${onChainFunds}")
                }
            }
            //FIXME(vicenzopalazzo) maybe in this section is possible  add more
            // information about status of channels, if the channel is ONCHAIN
            // is not correct calculate the sat here, right?
            if(jsonObject.has("channels")){
                val channels: JSONArray = jsonObject["channels"] as JSONArray
                for(i in 0 until channels.length()){
                    val channel = channels.getJSONObject(i)
                    offChainFunds = channel["channel_sat"] as Int
                    Log.d(TAG, "****** Off chain fund ${offChainFunds}")
                }
            }
            response.put("on_chain", onChainFunds)
            response.put("off_chain", offChainFunds)
            return response
        }
    }
}