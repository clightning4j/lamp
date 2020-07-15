package com.lvaccaro.lamp.util

import android.content.Context
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.toJSONObject
import com.sandro.bitcoinpaymenturi.BitcoinPaymentURI
import java.lang.Exception
import kotlin.collections.ArrayList

/**
 * @author https://github.com/vincenzopalazzo
 */
class Validator{

    companion object{

        val TAG = Validator.javaClass.canonicalName

        fun isBitcoinURL(text: String): Boolean{
            val result = doParseBitcoinURL(text)
            if(result.isNotEmpty()) return true //This is the cases it is the bitcoin URL
            return false
        }

        fun isBitcoinAddress(text: String): Boolean {
            //Support address P2PKH, P2MS && P2SH, P2WPKH, P2WSH
            return isP2PKH(text) || isP2SH(text) || isWitness(text)
        }

        private fun isP2SH(text: String): Boolean {
            val mainet = text.startsWith("3") && text.length == 34
            //FIXME (vincenzopalazzo): I noted that for the P2SH and witness script that used this convention
            //the address generated in testnet network in 1 character more large
            val testnet = text.startsWith("2") && text.length == 35
            return (mainet || testnet)
        }

        private fun isWitness(text: String): Boolean {
            val bitcoinFlag = "bc"
            val testnetFlag = "tb"
            val regtestFlag = "bcrt"

            val mainet = text.startsWith(bitcoinFlag) &&((text.length == 42) || (text.length == 62))
            val testnet = text.startsWith(testnetFlag) && ((text.length == 42) || (text.length == 62))
            //FIXME (vincenzopalazzo): I suppose the increase of address lenght because the flag is 2 character more large.
            val regtest = text.startsWith(regtestFlag) && ((text.length == 44) || (text.length == 64))
            return mainet || testnet || regtest
        }

        private fun isP2PKH(text: String): Boolean {
            val mainet = text.startsWith("1")
            val testnet = text.startsWith("m")
                            || text.startsWith("n")
            return (mainet ||testnet) && text.length == 34
        }

        fun doParseBitcoinURL(url: String) : HashMap<String, String>{
            val result = HashMap<String, String>()
            val bitcoinPaymentURI = BitcoinPaymentURI.parse(url)
            result.put(LampKeys.ADDRESS_KEY, bitcoinPaymentURI.address)
            val ammount = bitcoinPaymentURI.amount?.toString()
            if (ammount != null) {
                result.put(LampKeys.AMOUNT_KEY, ammount)
            }
            val label = bitcoinPaymentURI.label
            if (label != null) {
                result.put(LampKeys.LABEL_KEY, label)
            }
            val message = bitcoinPaymentURI.message
            if (message != null) {
                result.put(LampKeys.MESSAGE_KEY, message)
            }
            return result
        }

        fun isCorrectNetwork(cli: LightningCli, context: Context, address: String): String?{
            try{
                val rpcResponse = cli.exec(context, arrayOf("txprepare", address, "10000"), true).toJSONObject()
                return null //Address correct
            }catch (ex: Exception){
                //Address not correct
                if(ex.localizedMessage.contains("Cannot afford transaction")){
                    return null
                }
                return ex.localizedMessage
            }
        }

        //TODO (vincenzopalazzo) implement it
        //We know that the bolt start with ln and after with the network
        //for exampleL bc: for bitcoin and tb for testnet bitcoin
        fun isBolt11(string: String): Boolean{ return false }

        //TODO (vincenzopalazzo) implement it
        //It contains a @ and sometime a :
        //How is convention of the url?
        fun isLightningNodURI(string: String): Boolean {return false}
    }
}