package com.example.nfpet.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import java.nio.charset.Charset
import kotlin.experimental.and

object NfcParser {

    /**
     * Extracts and decodes NDEF payload from an incoming NFC Intent.
     * Returns the Pet ID if it was successfully extracted.
     */
    fun parseNdefIntent(intent: Intent): String? {
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == action
        ) {
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMsgs != null && rawMsgs.isNotEmpty()) {
                val msgs = Array(rawMsgs.size) { i -> rawMsgs[i] as NdefMessage }
                return parseNdefMessages(msgs)
            }
        }
        return null
    }

    private fun parseNdefMessages(messages: Array<NdefMessage>): String? {
        for (message in messages) {
            for (record in message.records) {
                val payload = record.payload
                val tnf = record.tnf
                
                // Parse URI Record
                if (tnf == NdefRecord.TNF_WELL_KNOWN && java.util.Arrays.equals(record.type, NdefRecord.RTD_URI)) {
                    val uri = parseUriRecord(payload)
                    // If it is our custom app scheme nfpet://profile/{id}, extract the ID
                    if (uri.startsWith("nfpet://profile/")) {
                        return uri.substringAfter("nfpet://profile/").trim()
                    }
                }
                
                // Parse Text Record
                if (tnf == NdefRecord.TNF_WELL_KNOWN && java.util.Arrays.equals(record.type, NdefRecord.RTD_TEXT)) {
                    return parseTextRecord(payload)
                }
            }
        }
        return null
    }

    private fun parseUriRecord(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val prefixCode = payload[0].toInt()
        val prefix = when (prefixCode) {
            0x00 -> ""
            0x01 -> "http://www."
            0x02 -> "https://www."
            0x03 -> "http://"
            0x04 -> "https://"
            else -> ""
        }
        val uriBytes = payload.copyOfRange(1, payload.size)
        return prefix + String(uriBytes, Charset.forName("UTF-8"))
    }

    private fun parseTextRecord(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        
        val statusByte = payload[0]
        val isUtf16 = (statusByte.toInt() and 0x80) != 0
        val languageCodeLength = (statusByte and 0x3F).toInt()
        
        val textEncoding = if (isUtf16) Charset.forName("UTF-16") else Charset.forName("UTF-8")
        val textBytes = payload.copyOfRange(1 + languageCodeLength, payload.size)
        
        return String(textBytes, textEncoding).trim()
    }
}
