/*
 * Copyright (c) 2017  Ohmer.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ohmerhe.kolley.upload

import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.ohmerhe.kolley.request.ByteRequest
import com.ohmerhe.kolley.request.Http
import java.io.*
import java.sql.Types

/**
 * Created by ohmer on 5/24/17.
 */

class UploadRequest(url: String, errorListener: Response.ErrorListener?)
    : ByteRequest(Request.Method.POST, url, errorListener) {
    private val PROTOCOL_CHARSET = "utf-8"
    private val curTime: Int = (System.currentTimeMillis() / 1000).toInt()
    private var boundaryPrefixed: String? = null

    init {
        boundaryPrefixed = MultipartUtils.BOUNDARY_PREFIX + curTime
    }

//    var fileParams: MutableMap<String, String> = mutableMapOf()
    var fileParams: ArrayList<Pair<String, String>> = ArrayList()

    /**
     * Upload progress
     */
    var _progress: (Int, Long) -> Unit = {write, total -> }

    /**
     * Returns the raw POST body to be sent.
     *
     * @throws AuthFailureError In the event of auth failure
     *
     * @deprecated Use {@link #getBody()} instead.
     */
    @Deprecated("", ReplaceWith("getBody()"))
    override fun getPostBody(): ByteArray? {
        return body
    }

    override fun getBodyContentType(): String {
        return String.format(MultipartUtils.CONTENT_TYPE_MULTIPART, PROTOCOL_CHARSET, curTime)
    }

    override fun getBody(): ByteArray? {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        try {
            buildParts(dos)
            return bos.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return super.getBody()
    }

    @Throws(IOException::class)
    private fun buildParts(dos: DataOutputStream) {
        val multipartParams = params
        val filesToUpload = fileParams

        for ((key, value) in multipartParams) {
            buildStringPart(dos, key, value)
        }

        for ((key, value) in filesToUpload) {

            val file = File(value)

            if (!file.exists()) {
                throw IOException(String.format("File not found: %s", file.absolutePath))
            } else if (file.isDirectory) {
                throw IOException(String.format("File is a directory: %s", file.absolutePath))
            }
            buildDataPart(dos, key, file)
        }

        // close multipart form data after text and file data
        dos.write((boundaryPrefixed + MultipartUtils.BOUNDARY_PREFIX).toByteArray())
        dos.write(MultipartUtils.CRLF.toByteArray())
    }

    @Throws(IOException::class)
    private fun buildStringPart(dataOutputStream: DataOutputStream, key: String, value: String) {

        dataOutputStream.write(boundaryPrefixed?.toByteArray())
        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())
        dataOutputStream.write(String.format(MultipartUtils.HEADER_CONTENT_DISPOSITION + MultipartUtils.COLON_SPACE + MultipartUtils.FORM_DATA, key).toByteArray())
        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())
        dataOutputStream.write((MultipartUtils.HEADER_CONTENT_TYPE + MultipartUtils.COLON_SPACE + MultipartUtils.CONTENT_TYPE_TEXT_PLAIN).toByteArray())
        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())
        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())
        dataOutputStream.write(value.toByteArray())
        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())
    }

    @Throws(IOException::class)
    private fun buildDataPart(dataOutputStream: DataOutputStream, key: String, file: File) {

        dataOutputStream.write(boundaryPrefixed?.toByteArray())
        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())
        dataOutputStream.write(String.format(MultipartUtils.HEADER_CONTENT_DISPOSITION + MultipartUtils.COLON_SPACE + MultipartUtils.FORM_DATA + MultipartUtils.SEMICOLON_SPACE + MultipartUtils.FILENAME, key, file.name).toByteArray())
        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())
        dataOutputStream.write((MultipartUtils.HEADER_CONTENT_TYPE + MultipartUtils.COLON_SPACE + MultipartUtils.CONTENT_TYPE_OCTET_STREAM).toByteArray())
        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())
        dataOutputStream.write((MultipartUtils.HEADER_CONTENT_TRANSFER_ENCODING + MultipartUtils.COLON_SPACE + Types.BINARY).toByteArray())
        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())
        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())

        val fileInputStream = FileInputStream(file)
        var bytesAvailable = fileInputStream.available()

        val maxBufferSize = 1024 * 1024
        var bufferSize = Math.min(bytesAvailable, maxBufferSize)
        val buffer = ByteArray(bufferSize)

        if (Http.DEBUG){
            Log.e("UploadRequest-->", "bytesAvailable : $bytesAvailable")
            Log.e("UploadRequest-->", "bufferSize : $bufferSize")
        }

        var bytesRead = fileInputStream.read(buffer, 0, bufferSize)

        val total = file.length()
        var count = 0
        _progress.invoke(count, total)

        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize)

            count += bytesRead
            _progress.invoke(count, total)

            if (Http.DEBUG) {
                Log.e("UploadRequest-->", "total : $total")
                Log.e("UploadRequest-->", "count : $count")
            }

            bytesAvailable = fileInputStream.available()
            bufferSize = Math.min(bytesAvailable, maxBufferSize)
            bytesRead = fileInputStream.read(buffer, 0, bufferSize)
        }

        dataOutputStream.write(MultipartUtils.CRLF.toByteArray())
    }
}