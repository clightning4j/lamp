package com.lvaccaro.lamp.util

import android.util.Log
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author https://github.com/vincenzopalazzo
 */
class Validator{

    companion object{

        val TAG = Validator.javaClass.canonicalName

        fun isBitcoinURL(text: String): Boolean{
            val result = doParseBitcoinURL(text)
            if(result.size == 2) return true //This is the cases it is the bitcoin URL
            return false
        }

        fun isBitcoinAddress(text: String): Boolean {
            //Support address P2PKH, P2MS && P2SH, P2WPKH, P2WSH
            return isP2PKH(text) || isP2SH(text) || isWitness(text);
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

        /**
         * The result contains the following information, with the following order
         * Index 0: Contains address
         * Index 1: Contains amount. This value exist if the string is an bitcoin url.
         * Index n: I'm missing somethings? FIXME: Double check
         *
         * If the string in incorrect, the result list is empty.
         */
        fun doParseBitcoinURL(url: String) : List<String>{
            val result = ArrayList<String>()
            //In this cases the url contains the follow patter
            //bitcoin:ADDRESS?=amount=VALUE
            var tokenizer = StringTokenizer(url, ":")
            // Match with the pattern STRING:STRING
            if(tokenizer.countTokens() == 2){
                //The variable tmp will contain all trash (not util) information
                var tmp = tokenizer.nextToken();
                Log.d(TAG, "**** Bitcoin protocol: ${tmp} *********")
                val queryString = tokenizer.nextToken()
                Log.d(TAG, "**** bitcoin protocol: ${queryString} *********")
                // Reassign the tokenizer variable a new token object
                tokenizer = StringTokenizer(queryString, "?")
                // Match with the pattern STRING?STRING
                if(tokenizer.countTokens() == 2){
                    var address =  tokenizer.nextToken();
                    Log.d(TAG, "******** Address inside URL ${address} ********")
                    result.add(address)
                    //Parsing the last part of URL
                    tmp = tokenizer.nextToken()
                    Log.d(TAG, "******** Amount request inside URL ${tmp} ********")
                    tokenizer = StringTokenizer(tmp, "=");
                    // Match with the pattern KEY=VALUE
                    if (tokenizer.countTokens() == 2){
                        //amount string
                        tmp = tokenizer.nextToken()
                        val amount = tokenizer.nextToken();
                        Log.d(TAG, "******** Amount inside URL ${address} ********")
                        result.add(amount)
                    }
                }
            }
            return result
        }
    }
}