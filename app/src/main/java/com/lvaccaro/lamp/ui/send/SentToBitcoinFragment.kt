package com.lvaccaro.lamp.ui.send

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.lvaccaro.lamp.LightningCli
import com.lvaccaro.lamp.R
import com.lvaccaro.lamp.toJSONObject
import com.lvaccaro.lamp.util.Validator
import org.json.JSONObject


/**
 * @author https://github.com/vincenzopalazzo
 */
class SentToBitcoinFragment : DialogFragment() {

    companion object{
        val TAG = SentToBitcoinFragment::class.java.canonicalName
    }

    lateinit var addressTextView: TextView
    lateinit var amountEditText: TextInputEditText
    lateinit var labelTextView: TextView
    lateinit var sendAllaCheckBox: CheckBox
    lateinit var cli: LightningCli

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sent_to_bitcoin, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        cli = LightningCli()

        addressTextView = view?.findViewById(R.id.textview_address_btc)!!
        addressTextView.text = arguments?.getString("address")
        amountEditText = view?.findViewById(R.id.edit_text_amount)!!
        amountEditText.setText(arguments?.getString("amount"))
        sendAllaCheckBox = view?.findViewById(R.id.check_send_all)!!
        sendAllaCheckBox.isChecked = amountEditText.text.toString().equals("all")

        val buttonSendBtc = view?.findViewById<Button>(R.id.button_send_bitcoin)!!
        buttonSendBtc.setOnClickListener{sendBitcoinToAddress()}
        val validAddress = Validator.isCorrectNetwork(cli,
            activity?.applicationContext!!, addressTextView.text.toString());
        if(validAddress != null){
            amountEditText.error = "ERROR FOUND"
            buttonSendBtc.isEnabled = false
            showSnackBar(validAddress, Snackbar.LENGTH_LONG)
        }
    }

    fun sendBitcoinToAddress(){
        lateinit var resultRPC: JSONObject
        try {
            resultRPC = cli.exec( context!!, arrayOf("withdraw", addressTextView.text.toString(),  amountEditText.text.toString()), true).toJSONObject()
            Log.d(TAG, resultRPC.toString())
            // TODO (vincenzopalazzo): add button copy to Snackbar
            showSnackBar(resultRPC["txid"].toString(), Snackbar.LENGTH_LONG)
            dismiss()
        }catch (ex: Exception){
            showToast(ex.localizedMessage, Toast.LENGTH_LONG)
            dismiss()
            ex.printStackTrace()
        }finally {

        }
    }

    private fun showSnackBar(message: String, duration: Int){
        Snackbar.make(view?.rootView!!, message, duration).show()
    }
    private fun showToast(message: String, duration: Int){
        Toast.makeText(view?.context, message, duration).show()
    }

}