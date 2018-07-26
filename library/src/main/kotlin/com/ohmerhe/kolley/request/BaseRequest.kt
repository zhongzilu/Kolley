/*
 * Copyright (c) 2016  Ohmer.
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

package com.ohmerhe.kolley.request

import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser

/**
 * Created by ohmer on 4/14/16.
 */
open class ByteRequest(method: Int, url: String, errorListener: Response.ErrorListener? = Response.ErrorListener {})
: BaseRequest<ByteArray>(method, url, errorListener) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<ByteArray>? {
        return Response.success(response?.data, HttpHeaderParser.parseCacheHeaders(response))
    }
}

abstract class BaseRequest<D>(method: Int, url: String, errorListener: Response.ErrorListener? = Response.ErrorListener {})
: Request<D>(method, url, errorListener) {
    protected val DEFAULT_CHARSET = "UTF-8"

    internal var _listener: Response.Listener<D>? = null
    protected val _heads: MutableMap<String, String> = mutableMapOf()
    protected val _params: MutableMap<String, String> = mutableMapOf() // used for a POST or PUT request.

    /**
     * Returns a Map of parameters to be used for a POST or PUT request.
     * @return
     */
    public override fun getParams(): MutableMap<String, String> {
        return _params
    }

    override fun getHeaders(): MutableMap<String, String> {
        return _heads
    }

    fun setHeaders(heads: MutableMap<String, String>){
        _heads.putAll(heads)
    }

    fun setParams(params: MutableMap<String, String>){
        _params.putAll(params)
    }

    override fun deliverResponse(response: D?) {
        _listener?.onResponse(response)
    }

    protected fun log(msg: String) {
        if (Http.DEBUG) {
            Log.d(this.javaClass.simpleName, msg)
        }
    }

    open fun setRetryPolicy(timeout: Int = DefaultRetryPolicy.DEFAULT_TIMEOUT_MS): Request<*> {
        return super.setRetryPolicy(DefaultRetryPolicy(timeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
    }
}

