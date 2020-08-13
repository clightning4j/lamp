package com.lvaccaro.lamp.util

import android.content.Context
import android.util.Log
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.toJSONObject
import java.util.*
import kotlin.collections.HashMap


/**
 * @author https://github.com/vincenzopalazzo
 */
class Validator {

    companion object {

        val TAG = Validator.javaClass.canonicalName

        fun isBitcoinURL(text: String): Boolean {
            val result = doParseBitcoinURL(text)
            if (result.isNotEmpty()) return true //This is the cases it is the bitcoin URL
            return false
        }

        fun isBitcoinAddress(text: String): Boolean {
            //Support address P2PKH, P2MS && P2SH, P2WPKH, P2WSH
            return isP2PKH(text) || isP2SH(text) || isWitness(text)
        }

        private fun isP2SH(text: String): Boolean {
            val mainet = text.startsWith("3") && text.length == 34
            //the address generated in testnet network in 1 character more large
            val testnet = text.startsWith("2") && text.length == 35
            return (mainet || testnet)
        }

        private fun isWitness(text: String): Boolean {
            val bitcoinFlag = "bc"
            val testnetFlag = "tb"
            val regtestFlag = "bcrt"

            val mainet =
                text.startsWith(bitcoinFlag) && ((text.length == 42) || (text.length == 62))
            val testnet =
                text.startsWith(testnetFlag) && ((text.length == 42) || (text.length == 62))
            val regtest =
                text.startsWith(regtestFlag) && ((text.length == 44) || (text.length == 64))
            return mainet || testnet || regtest
        }

        private fun isP2PKH(text: String): Boolean {
            val mainet = text.startsWith("1")
            val testnet = text.startsWith("m")
                    || text.startsWith("n")
            return (mainet || testnet) && text.length == 34
        }

        fun doParseBitcoinURL(url: String): HashMap<String, String> {
            val result = HashMap<String, String>()
            if(url.trim().isEmpty()) return result
            //In this cases the url contains the follow patter
            //bitcoin:ADDRESS?amount=VALUE
            var tokenizer = StringTokenizer(url.trim().replace("%20", " "), ":")
            // Match with the pattern STRING:STRING
            if (tokenizer.countTokens() == 2) {
                //The variable tmp will contain all trash (not util) information
                var tmp = tokenizer.nextToken()
                Log.d(TAG, "**** Bitcoin protocol: ${tmp} *********")
                if(!isProtocolPrefix(tmp)) return result;
                val queryString = tokenizer.nextToken()
                Log.d(TAG, "**** bitcoin URI: ${queryString} *********")
                // Reassign the tokenizer variable a new token object
                tokenizer = StringTokenizer(queryString, "?")
                // Match with the pattern STRING?STRING
                when(tokenizer.countTokens()){
                    1 -> {
                        var address = tokenizer.nextToken()
                        Log.d(TAG, "******** Address inside URL ${address} ********")
                        result.put(LampKeys.ADDRESS_KEY, address)
                    }
                    2 -> {
                        var address = tokenizer.nextToken()
                        Log.d(TAG, "******** Address inside URL ${address} ********")
                        result.put(LampKeys.ADDRESS_KEY, address)
                        //Parsing parameter URI
                        tmp = tokenizer.nextToken()
                        Log.d(TAG, "******** Parameters inside URL ${tmp} ********")
                        tokenizer = StringTokenizer(tmp, "&")
                        while (tokenizer.hasMoreTokens()){
                            val parameter = tokenizer.nextToken()
                            Log.d(TAG, "Parameter ${parameter}")
                            if(!parseParameter(parameter, result)) break
                        }
                    }
                }
            }
            return result
        }

        private fun isProtocolPrefix(protocol: String?): Boolean {
            return protocol.equals("bitcoin", false) ||
                    protocol.equals("lightning", false)
        }

        private fun parseParameter(uri: String, result: HashMap<String, String>): Boolean{
            val tokes = StringTokenizer(uri, "=")
            if (tokes.countTokens() == 2) {
                val key = tokes.nextToken()
                Log.d(TAG, "******** key ${key} ********")
                val value = tokes.nextToken();
                Log.d(TAG, "******** Value ${value} ********")
                result.put(key, value)
                return true
            }
            return false
        }

        fun isCorrectNetwork(cli: LightningCli, context: Context, address: String): String? {
            try {
                val rpcResult = cli.exec(context, arrayOf("txprepare", address, "1000"), true).toJSONObject()
                //release the input to see the correct balance inside the output
                cli.exec(context, arrayOf("txdiscard", rpcResult["txid"].toString()), true).toJSONObject()
                return null //Address correct
            } catch (ex: Exception) {
                //not enough bitcoin to create the transaction but the address was correct
                if (ex.localizedMessage.contains("Cannot afford transaction")) {
                    return null
                }
                //Address not correct
                return ex.localizedMessage
            }
        }

        fun isBolt11(string: String): Boolean {
            // These variable (step and len) are used to check the type of invoice
            // I need this variable because the prifix from (mainet, testnet) to regtest
            // have different length.
            // This function check the format bolt11 mensioned inside the bolt11.
            // other sanity check will do from lightningd daemon.
            var step = 2
            var len = 6
            if(string.startsWith("lnbcrt")){
                step = 4
                len = 7;
            }
            val startString = string.subSequence(0, len)
            Log.d(TAG, "First subsequences is ${startString}")
            val lnNetwork = startString.subSequence(0, step)
            Log.d(TAG, "Network tag ${lnNetwork}")
            val network = startString.subSequence(step, step + 2)
            step += 2
            Log.d(TAG, "Network tag is ${network}")
            // minimum cases, the time stamp can be more big but not more small of one digit
            val timestamp = startString.subSequence(step, step + 1)
            //val timestampIsNum = timestamp.matches("-?\\d+(\\.\\d+)?".toRegex()) NOT HUMAN READBLE
            var timestampIsNum = true
            timestamp.forEach find@{ charatter ->
                if (charatter.isLetter()){
                    timestampIsNum = false
                    return@find
                }
            }
            Log.d(TAG, "Timestamp is ${timestamp}")
            Log.d(TAG, "The timestamp contains all numbers? ${timestampIsNum}")
            return isPrefix("ln", lnNetwork) &&
                    (isPrefix("bc", network)
                            || isPrefix("tb", network))
                      && timestampIsNum
        }

        private fun isPrefix(prefix: String, result: CharSequence): Boolean {
            return prefix.equals(result)
        }

        fun isLightningNodURI(string: String): Boolean {
            //FIXME: Check more details about the node id
            // for example is possible check if there is some illegal value like ? or letter with upper case
            val patternNode = "0360dca2f35336d303643a7fb172ba6185b9086aa1fbd6063a1447050f2dda0f87"
            if (string.contains("@")) {
                val token = StringTokenizer(string, "@")
                if (token.countTokens() == 2) {
                    val nodeid = token.nextToken()
                    val networkInfo = token.nextToken()
                    return (networkInfo.contains(":") || networkInfo.contains(".onion")) &&
                            nodeid.length == patternNode.length
                }
            } else {
                // is only a node id?
                return string.trim().length == patternNode.length
            }
            // No node id
            return false
        }
    }
}